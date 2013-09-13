package sim;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import sim.control.GuiState;
import sim.control.ResourceManager;
import sim.control.TacticalWorker;
import sim.model.Agent;
import sim.model.Agent.MovementBehavior;
import sim.model.Board;
import sim.model.Mall;
import sim.model.algo.Spawner;
import sim.model.helpers.Rand;
import sim.util.AviRecorder;

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
	private int AGENT_GENERATION_INTERVAL = 2;

	/**
	 * Maximal number of people in a mall as a percentage of accessible area.
	 */
	private double MAX_CROWD_FACTOR = 0.15;

	private Mall mall = new Mall();
	private AviRecorder aviRecorder;

	private int stepCounter = 0;

	/**
	 * Index of the last step when an agent has been generated.
	 */
	private int lastGenerationStep = 0;

	public Simulation(AviRecorder aviRecorder) {
		super();
		this.aviRecorder = aviRecorder;
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
						// Agent w pobliżu celu?
						final double maxDistanceFromTarget = 2;
						double dist = agent.getTarget().distance(curr);
						if (dist < maxDistanceFromTarget) {
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
	private void prepareAgents() {
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
		Board board = mall.getBoard();

		// Number of agents who successfully reached their final destination.
		int nAgentSuccesses = 0;

		Point p = new Point();
		for (int y = 0; y < mall.getBoard().getHeight(); y++)
			for (int x = 0; x < mall.getBoard().getWidth(); x++) {
				p.setLocation(x, y);
				mall.getBoard().getCell(p).clearVisitsCounter();
			}

		// ResourceManager.randomize(board, board.getHeight() * board.getWidth()
		// / 50);
		// XXX: ta metoda najprawdopodobniej musi zostać zmodyfikowana
		computePaths();

		int nAgentsBegin = mall.getBoard().countAgents();
		nTotalAgents += nAgentsBegin;

		// Ilość agentów, którzy osiągnęli swój cel.
		int targetsReached = 0;

		for (stepCounter = 0; stepCounter < STEPS; stepCounter++) {
			if (stepCounter % aviRecorder.getSimFramesPerAviFrame() == 0)
				aviRecorder.recordFrame();

			generateAgent();

			targetsReached = computeTargetReached();

			if (targetsReached == nAgentsBegin) {
				// nAgentSuccesses += nAgentsBegin;
				// XXX: poniższy warunek został okomentowany na potrzeny testów
				// break;
			}

			try {
				Thread.sleep(GuiState.animationSpeed);
			} catch (InterruptedException e) {
			}

			prepareAgents();

			Map<Agent, Integer> speedPointsLeft = computeMovementPointsLeft();
			moveAgents(speedPointsLeft);
			
			clearAgentsOnExits();
		}

		nAgentSuccesses += targetsReached;

		System.out.println(String.format("Sukcesy agentów:\t %d / %d\t (%d%%)",
				nAgentSuccesses, nTotalAgents, nAgentSuccesses * 100
						/ nTotalAgents));
	}
	
	private void clearAgentsOnExits() {
		Board board = mall.getBoard();
		Point p = new Point();
		for (int x = 0; x < board.getWidth(); x++) {
			for (int y = 0; y < board.getHeight(); y++) {
				p.setLocation(x, y);
				if (board.getCell(p).getFeature() instanceof Spawner && board.getCell(p).getAgent() != null)
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
		BlockingQueue<Agent> agentsToCompute = new LinkedBlockingQueue<>(2);
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
}