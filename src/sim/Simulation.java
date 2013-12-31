package sim;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import sim.control.GuiState;
import sim.control.TacticalWorker;
import sim.gui.SummaryTable;
import sim.gui.SummaryTable.Param;
import sim.model.Agent;
import sim.model.Agent.MovementBehavior;
import sim.model.Board;
import sim.model.Mall;
import sim.model.algo.Ped4.LaneDirection;
import sim.model.helpers.Direction;
import sim.model.helpers.Rand;
import sim.util.WriterUtils;
import sim.util.video.VideoRecorder;

import com.google.common.collect.Iterables;

public class Simulation extends Observable implements Runnable {

	/**
	 * Number of threads used to compute path (in tactical part).
	 */
	public static final int NUM_TACTICAL_THREADS = 4;

	/**
	 * Maxmial number of steps in a single simulation.
	 */
	private final int STEPS = 5000;

	/**
	 * Interval (= number of simulation steps) between appearance of a next
	 * agent.
	 */
	private final double NEW_AGENTS_PER_ITERATION = 4.1;

	/**
	 * Maximal number of people in a mall as a percentage of accessible area.
	 */
	private double MAX_CROWD_FACTOR = 0.15;

	/**
	 * In this radius it is possible for an agent to mark its current target as
	 * visited.
	 */
	private final int MAX_DISTANCE_FROM_TARGET = 2;

	/**
	 * Width of an assessment frame is ~10m. (Its height depends corridor's
	 * breadth)
	 */
	private final int ASSESSMENT_FRAME_WIDTH = 15;

	private Mall mall = new Mall();
	private VideoRecorder videoRecorder;

	private int stepCounter = 0;

	private BlockingQueue<Agent> agentsToCompute = new LinkedBlockingQueue<>(1);

	// Aktualny stan "puli" nowych agentów.
	private double newAgentLevel = 0.0;

	private BufferedWriter logWriter = null;

	private final static Logger LOGGER = Logger
			.getLogger(Logger.GLOBAL_LOGGER_NAME);

	public Simulation(VideoRecorder videoRecorder) {
		super();
		this.videoRecorder = videoRecorder;
	}

	public void configureLogFile() {
		if (logWriter != null) {
			try {
				logWriter.close();
			} catch (IOException e) {
			}
		}

		try {
			Files.createDirectories(Paths.get("logs"));
			
			File logFile = new File("./logs/" + System.currentTimeMillis() + ".csv");
			Charset charset = Charset.forName("US-ASCII");

			logWriter = Files.newBufferedWriter(logFile.toPath(), charset);
			logWriter
					.write("\"% of fields as lanes\";\"lanes coherence\";\"lost\";\"avg dist\"\r\n");
		} catch (IOException e) {
			LOGGER.severe("Could not open log file for writing.");
		}

	}

	public Mall getMall() {
		return mall;
	}

	public void setMall(Mall mall) {
		this.mall = mall;
	}

	/**
	 * Sprawdza, czy agenci dotarli do celu (jeśli tak - uaktualnia cele).
	 * 
	 * @return ilość agentów, którzy dotarli do ostatecznego celu
	 */
	private int computeTargetReached() {
		int targetsReached = 0;

		// Sprawdzanie, czy cel został osiągnięty.
		Point curr = new Point();
		Agent agent;
		for (int y = 0; y < mall.getBoard().getHeight(); y++) {
			for (int x = 0; x < mall.getBoard().getWidth(); x++) {
				curr.setLocation(x, y);
				agent = mall.getBoard().getCell(curr).getAgent();

				if (agent == null)
					continue;

				if (agent.getTargetCount() > 0) {
					if (agent.getTarget().equals(curr)) {
						reachTarget(agent);
					} else {
						double dist = agent.getTarget().distance(curr);
						if (dist < MAX_DISTANCE_FROM_TARGET) {
							// TODO: metoda probabilistyczna
							if (Rand.nextDouble() < 1 / (dist * dist)) {
								reachTarget(agent);
							}
						}
					}
				} else {
					// target count == 0
					targetsReached++;
				}
			}
		}

		return targetsReached;
	}

	private void reachTarget(Agent agent) {
		Point p = agent.getPosition();
		agent.reachTarget();
		if (agent.getTargetCount() > 0) {
			Point t = agent.getTarget();

			// metryka miejska
			int dist = Math.abs(p.x - t.x) + Math.abs(p.y - t.y);
			agent.setInitialDistanceToTarget(dist);
		}
	}

	/**
	 * Prepare all agents on the mall.getBoard() for the next step.
	 */
	private void prepareAgentsForNextStep() {
		Point p = new Point();
		for (int y = 0; y < mall.getBoard().getHeight(); y++) {
			for (int x = 0; x < mall.getBoard().getWidth(); x++) {
				p.setLocation(x, y);
				Agent agent = mall.getBoard().getCell(p).getAgent();

				if (agent != null && agent.getTargetCount() > 0) {
					if (!agent.getTarget().equals(p))
						mall.getBoard().getCell(p).getAlgorithm()
								.prepare(mall.getBoard(), agent);
				}

			}
		}

		this.setChanged();
		this.notifyObservers();
	}

	private Map<Agent, Integer> computeMovementPointsLeft() {
		Point p = new Point();
		Map<Agent, Integer> speedPointsLeft = new WeakHashMap<Agent, Integer>();
		for (int y = 0; y < mall.getBoard().getHeight(); y++) {
			for (int x = 0; x < mall.getBoard().getWidth(); x++) {
				p.setLocation(x, y);
				Agent agent = mall.getBoard().getCell(p).getAgent();

				if (agent != null)
					speedPointsLeft.put(agent, agent.getvMax());
			}
		}

		return speedPointsLeft;
	}

	private void moveAgents(Map<Agent, Integer> speedPointsLeft) {
		Point p = new Point();
		Set<Agent> moved = new HashSet<Agent>();

		for (int step = 0; step < Agent.V_MAX; step++) {
			moved.clear();
			for (int y = 0; y < mall.getBoard().getHeight(); y++) {
				for (int x = 0; x < mall.getBoard().getWidth(); x++) {
					p.setLocation(x, y);
					Agent a = mall.getBoard().getCell(p).getAgent();

					if (a == null || moved.contains(a))
						continue;

					if (a.getHoldTime() > 0) {
						a.decrementHoldTime();
						continue;
					}

					// Agent osiągnął swój końcowy cel.
					if (a.getTargetCount() == 0 || a.getTarget().equals(p))
						continue;

					moved.add(a);

					if (speedPointsLeft.get(a) > 0) {
						// XXX: w przyszłości można tu dodać model
						// probabilistyczny (aby uzyskać w miarę
						// równomierny rozkład wykonanych kroków w
						// czasie)

						mall.getBoard()
								.getCell(p)
								.getAlgorithm()
								.nextIterationStep(mall.getBoard(), a,
										speedPointsLeft);
						mall.getBoard().getCell(a.getPosition()).getFeature()
								.performAction(a);

						speedPointsLeft.put(a, speedPointsLeft.get(a) - 1);
					}
				}
			}
		}

		this.setChanged();
		this.notifyObservers();
	}

	@Override
	public void run() {
		int nTotalAgents = 0;

		// Number of agents who successfully reached their final destination.
		int nAgentSuccesses = 0;

		// Ilość agentów, którzy osiągnęli swój cel.
		int targetsReached = 0;

		prepareBoardForNextStep();

		// ResourceManager.randomize(board, board.getHeight() * board.getWidth()
		// / 50);
		// XXX: ta metoda najprawdopodobniej musi zostać zmodyfikowana
		computePaths();

		int nAgentsBegin = mall.getBoard().countAgents();
		nTotalAgents += nAgentsBegin;

		for (stepCounter = 0; stepCounter < STEPS; stepCounter++) {
			if (stepCounter % videoRecorder.getSimFramesPerAviFrame() == 0)
				videoRecorder.recordFrame();

			generateAgents();

			targetsReached = computeTargetReached();

			try {
				Thread.sleep(GuiState.animationSpeed);
			} catch (InterruptedException e) {
			}

			prepareAgentsForNextStep();

			Map<Agent, Integer> speedPointsLeft = computeMovementPointsLeft();
			moveAgents(speedPointsLeft);

			clearAgentsOnExits();

			assessPed4();
			assessSocialDistances();
		}

		nAgentSuccesses += targetsReached;

		System.out.println(String.format("Sukcesy agentów:\t %d / %d\t (%d%%)",
				nAgentSuccesses, nTotalAgents, nAgentSuccesses * 100
						/ nTotalAgents));
	}

	private void assessSocialDistances() {
		Board board = mall.getBoard();
		Point p = new Point();
		SummaryTable summary = MallSim.getFrame().getSummaryTable();

		int lost = 0;
		double avgWalkingDistance = 0.0;
		int nAgents = 0;

		for (int x = 0; x < board.getWidth(); x++) {
			for (int y = 0; y < board.getHeight(); y++) {
				p.setLocation(x, y);

				Agent a = board.getCell(p).getAgent();
				if (a == null) {
					continue;
				}

				nAgents++;

				if (a.isLost())
					lost++;

				if (a.getFieldsMoved() < a.getInitialDistanceToTarget()
						|| a.getInitialDistanceToTarget() == 0.0) {
					avgWalkingDistance += 1.0;
				} else {
					Double d = a.getFieldsMoved()
							/ (double) a.getInitialDistanceToTarget();

					avgWalkingDistance += (d.isInfinite() || d.isNaN()) ? 1.0
							: d;
				}
			}
		}

		avgWalkingDistance /= (double) nAgents;

		summary.setParamValue(Param.LOST, lost);
		summary.setParamValue(Param.AVG_DISTANCE, avgWalkingDistance);

		summary.nextSample();

		try {
			if (logWriter != null)
				logWriter.write(String.format("%d;%s\r\n", lost,
						WriterUtils.decimalFormat.format(avgWalkingDistance)));
		} catch (IOException e) {
		}
	}

	private void prepareBoardForNextStep() {
		Point p = new Point();
		for (int y = 0; y < mall.getBoard().getHeight(); y++) {
			for (int x = 0; x < mall.getBoard().getWidth(); x++) {
				p.setLocation(x, y);
				mall.getBoard().getCell(p).clearVisitsCounter();
			}
		}
	}

	private void assessPed4() {
		Board board = mall.getBoard();
		Point p = new Point();
		SummaryTable summary = MallSim.getFrame().getSummaryTable();

		// TODO: enum map (ile kratek danego typu)

		int left = 0;
		int right = 0;
		int none = 0;

		for (int x = 0; x < board.getWidth(); x++) {
			for (int y = 0; y < board.getHeight(); y++) {
				p.setLocation(x, y);

				LaneDirection dir = assessRow(y, x);

				switch (dir) {
				case EAST:
					right++;
					break;
				case WEST:
					left++;
					break;
				case NONE:
					none++;
					break;
				default:
					break;
				}

				board.getCell(p).setLaneDirection(dir);
			}
		}

		double all = left + right + none;
		double perc = (all == 0) ? 100.0 : (all - none) / all * 100.0;

		summary.setParamValue(Param.PERC_OF_FIELDS_AS_LANES, perc);

		// Coherence - miara spójnosci alejek (niespójnosc pojawia sie, gdy
		// jeden pas otoczony jest dwoma innymi o przeciwnym kierunku (EWE albo
		// WEW).
		Point p0 = new Point();
		Point p2 = new Point();
		int coherence = 0;
		for (int x = 0; x < board.getWidth(); x++) {
			for (int y = 1; y < board.getHeight() - 1; y++) {
				p0.setLocation(x, y - 1);
				p.setLocation(x, y);
				p2.setLocation(x, y + 1);

				LaneDirection dir1 = board.getCell(p0).getLaneDirection();
				LaneDirection dir2 = board.getCell(p).getLaneDirection();
				LaneDirection dir3 = board.getCell(p2).getLaneDirection();

				if (dir1.isDirection() && dir2.isDirection()
						&& dir2.isDirection() && dir1 == dir3 && dir1 != dir2)
					coherence--;
			}
		}
		summary.setParamValue(Param.LANES_COHERENCE, coherence);

		summary.nextSample();

		try {
			if (logWriter != null)
				logWriter.write(String.format("%s;%d;",
						WriterUtils.decimalFormat.format(perc), coherence));
		} catch (IOException e) {
		}
	}

	private void clearAgentsOnExits() {
		Board board = mall.getBoard();
		Point p = new Point();
		Agent agent = null;
		for (int x = 0; x < board.getWidth(); x++) {
			for (int y = 0; y < board.getHeight(); y++) {
				p.setLocation(x, y);
				agent = board.getCell(p).getAgent();

				boolean isAgentOnExit = false;// board.getCell(p).getFeature()
												// instanceof Spawner && agent
												// != null &&
												// agent.getTargetCount() == 0;
				boolean isAgentNearExit = agent != null
						&& agent.getTargetCount() == 0;
				if (isAgentOnExit || isAgentNearExit) {
					board.setAgent(null, p);
				}
			}
		}
	}

	private void computePaths() {
		BlockingQueue<Agent> agentsToCompute = new LinkedBlockingQueue<>(5);
		List<TacticalWorker> threads = new ArrayList<>(NUM_TACTICAL_THREADS);

		for (int i = 0; i < NUM_TACTICAL_THREADS; i++) {
			TacticalWorker t = new TacticalWorker(agentsToCompute,
					mall.getBoard());
			threads.add(t);
			t.start();
		}

		if (mall.getBoard().countAgents() == 0) {
			mall.getBoard().setAgent(new Agent(MovementBehavior.DYNAMIC),
					new Point(2, 2));
		}

		Point p = new Point();
		for (int y = 0; y < mall.getBoard().getHeight(); y++) {
			for (int x = 0; x < mall.getBoard().getWidth(); x++) {
				p.setLocation(x, y);
				Agent a = mall.getBoard().getCell(p).getAgent();
				if (a != null) {
					try {
						agentsToCompute.put(a);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		for (TacticalWorker t : threads) {
			t.setStopped();
		}
	}

	private void computePaths(Agent agent) {
		try {
			agentsToCompute.put(agent);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		TacticalWorker t = new TacticalWorker(agentsToCompute, mall.getBoard());
		t.start();
		t.setStopped();
	}

	private void generateAgents() {
		Board board = mall.getBoard();

		newAgentLevel += NEW_AGENTS_PER_ITERATION;

		while (newAgentLevel >= 1.0) {
			boolean isTooCrowdy = board.countAgents()
					/ (double) board.getAccessibleFieldCount() >= MAX_CROWD_FACTOR;

			if (isTooCrowdy)
				break;

			List<Point> ioPoints = board.getIoPoints();

			// Select an empty field.
			Collections.shuffle(ioPoints);
			for (Point p : ioPoints) {
				if (board.getCell(p).getAgent() == null) {
					Agent agent = new Agent(MovementBehavior.AVERAGE);
					board.setAgent(agent, p);
					computePaths(agent);

					// TODO: wyznaczyć zachowanie agenta

					break;
				}
			}

			newAgentLevel -= 1.0;
		}
	}

	private void testAssessment() {
		Board b = mall.getBoard();
		b.reset();

		Agent aN = new Agent(MovementBehavior.AVERAGE);
		Agent aS = new Agent(MovementBehavior.AVERAGE);
		Agent aW = new Agent(MovementBehavior.AVERAGE);
		Agent aE = new Agent(MovementBehavior.AVERAGE);

		aN.setDirection(Direction.N);
		aS.setDirection(Direction.S);
		aW.setDirection(Direction.W);
		aE.setDirection(Direction.E);

		// Agent[] agents = new Agent[] {
		// aE, aE, aE, aW, aE, aN, aE, aW, aE, aE, aE
		// };

		Agent[] agents = new Agent[] { aW, aW, aS, aW, aW, aS, aW };

		for (int i = 0; i < agents.length; i++) {
			b.setAgent(new Agent(agents[i]), new Point(i + 2, 1));
		}

		assessRow(1, 1);
	}

	private LaneDirection assessRow(int rowIndex, int columnIndex) {
		int sumOfDirections = 0;
		int nAgents = 0; // number of agents
		List<Direction> groupDirections = new ArrayList<>();
		LinkedList<Integer> groupSizes = new LinkedList<>();

		Point p = new Point();

		int startCol = Math.max(0, columnIndex - ASSESSMENT_FRAME_WIDTH / 2);
		int endCol = Math.min(mall.getBoard().getWidth(), startCol
				+ ASSESSMENT_FRAME_WIDTH);

		for (int colInx = startCol; colInx < endCol; colInx++) {
			p.setLocation(colInx, rowIndex);
			Agent agent = mall.getBoard().getCell(p).getAgent();

			if (agent == null)
				continue;

			nAgents++;

			sumOfDirections += agent.getDirection().getVec().x;

			Direction lastGroupDirection = Iterables.getLast(groupDirections,
					null);
			boolean isSameDirection = lastGroupDirection == agent
					.getDirection();
			boolean bothVertical = lastGroupDirection == null ? false
					: lastGroupDirection.isVertical()
							&& agent.getDirection().isVertical();
			if (!(isSameDirection || bothVertical)) {
				// start new group
				groupDirections.add(agent.getDirection());
				groupSizes.add(0);
			}

			Integer size = groupSizes.getLast();
			groupSizes.removeLast();
			groupSizes.addLast(size + 1);
		}

		double dominantDirection = sumOfDirections / (double) nAgents;

		double avgGroupSize = 0;
		for (Integer s : groupSizes)
			avgGroupSize += s;
		avgGroupSize /= groupSizes.size();

		double varWest = 0.0;
		double varEast = 0.0;
		for (int i = 0; i < groupSizes.size(); i++) {
			int s = groupSizes.get(i);
			if (groupDirections.get(i) == Direction.W)
				varWest += (s - avgGroupSize) * (s - avgGroupSize);
			else if (groupDirections.get(i) == Direction.E)
				varEast += (s - avgGroupSize) * (s - avgGroupSize);
		}

		double density = nAgents / (double) ASSESSMENT_FRAME_WIDTH;

		boolean hasDirection = (density < 0.6)
				&& (Math.abs(dominantDirection) > 0.6);

		if (groupSizes.isEmpty())
			return LaneDirection.EMPTY;

		if (hasDirection) {
			return (dominantDirection > 0.0) ? LaneDirection.EAST
					: LaneDirection.WEST;
		} else {
			return LaneDirection.NONE;
		}
	}

	public void finish() {
		if (logWriter != null) {
			try {
				logWriter.close();
			} catch (IOException e) {
			}
		}
	}
}