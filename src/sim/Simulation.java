package sim;

import java.awt.Point;
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

import sim.control.GuiState;
import sim.control.TacticalWorker;
import sim.model.Agent;
import sim.model.Agent.MovementBehavior;
import sim.model.Board;
import sim.model.Mall;
import sim.model.algo.Ped4.LaneDirection;
import sim.model.helpers.Direction;
import sim.model.helpers.Rand;
import sim.util.video.AviRecorder;
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
	private int AGENT_GENERATION_INTERVAL = 1;

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

	/**
	 * Index of the last step when an agent has been generated.
	 */
	private int lastGenerationStep = 0;

	private BlockingQueue<Agent> agentsToCompute = new LinkedBlockingQueue<>(1);

	public Simulation(VideoRecorder videoRecorder) {
		super();
		this.videoRecorder = videoRecorder;
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
						agent.reachTarget();
						if (agent.getTargetCount() > 0)
							agent.setInitialDistanceToTarget(curr
									.distanceSq(agent.getTarget()));
					} else {
						double dist = agent.getTarget().distance(curr);
						if (dist < MAX_DISTANCE_FROM_TARGET) {
							// TODO: metoda probabilistyczna
							if (Rand.nextDouble() < 1 / (dist * dist)) {
								agent.reachTarget();

								if (agent.getTargetCount() > 0)
									agent.setInitialDistanceToTarget(curr
											.distanceSq(agent.getTarget()));
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

			generateAgent();

			targetsReached = computeTargetReached();

			try {
				Thread.sleep(GuiState.animationSpeed);
			} catch (InterruptedException e) {
			}

			prepareAgentsForNextStep();

			Map<Agent, Integer> speedPointsLeft = computeMovementPointsLeft();
			moveAgents(speedPointsLeft);

			clearAgentsOnExits();

			assess();
		}

		nAgentSuccesses += targetsReached;

		System.out.println(String.format("Sukcesy agentów:\t %d / %d\t (%d%%)",
				nAgentSuccesses, nTotalAgents, nAgentSuccesses * 100
						/ nTotalAgents));
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

	private void assess() {
		Board board = mall.getBoard();
		Point p = new Point();
		// TODO: enum map (ile kratek danego typu)

		for (int x = 0; x < board.getWidth(); x++)
			for (int y = 0; y < board.getHeight(); y++) {
				p.setLocation(x, y);
				board.getCell(p).setLaneDirection(assessRow(y, x));
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
				if (isAgentOnExit || isAgentNearExit)
					board.getCell(p).setAgent(null);
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

	private void generateAgent() {
		Board board = mall.getBoard();

		boolean isTooEarly = stepCounter - lastGenerationStep < AGENT_GENERATION_INTERVAL;
		boolean isTooCrowdy = board.countAgents()
				/ (double) board.getAccessibleFieldCount() >= MAX_CROWD_FACTOR;

		if (isTooEarly || isTooCrowdy)
			return;

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

		lastGenerationStep = stepCounter;
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

		// System.out.println("density = " + density);
		// System.out.println("dominantDirection = " + dominantDirection);
		// System.out.println("varWest = " + varWest);
		// System.out.println("varEast = " + varEast);
		// System.exit(0);
	}
}