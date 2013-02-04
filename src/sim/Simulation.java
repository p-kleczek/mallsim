package sim;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
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
import sim.model.helpers.Misc;
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

	private Mall mall;
	private Board board;
	private AviRecorder aviRecorder;

	public Simulation(Mall mall, AviRecorder aviRecorder) {
		super();
		this.mall = mall;
		board = mall.getBoard();
		this.aviRecorder = aviRecorder;
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
		for (int y = 0; y < board.getHeight(); y++) {
			for (int x = 0; x < board.getWidth(); x++) {
				curr.setLocation(x, y);
				agent = board.getCell(curr).getAgent();

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
	 * Prepare all agents on the board for the next step.
	 */
	private void prepareAgents() {
		Point p = new Point();
		for (int y = 0; y < board.getHeight(); y++) {
			for (int x = 0; x < board.getWidth(); x++) {
				p.setLocation(x, y);
				Agent agent = board.getCell(p).getAgent();

				if (agent != null && agent.getTargetCount() > 0) {
					if (!agent.getTarget().equals(p))
						board.getCell(p).getAlgorithm().prepare(agent);
				}

			}
		}

		this.setChanged();
		this.notifyObservers();
	}

	private Map<Agent, Integer> computeMovementPointsLeft() {
		Point p = new Point();
		Map<Agent, Integer> speedPointsLeft = new WeakHashMap<Agent, Integer>();
		for (int y = 0; y < board.getHeight(); y++) {
			for (int x = 0; x < board.getWidth(); x++) {
				p.setLocation(x, y);
				Agent agent = board.getCell(p).getAgent();

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
			for (int y = 0; y < board.getHeight(); y++) {
				for (int x = 0; x < board.getWidth(); x++) {
					p.setLocation(x, y);
					Agent a = board.getCell(p).getAgent();

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

						board.getCell(p).getAlgorithm()
								.nextIterationStep(a, speedPointsLeft);
						board.getCell(a.getPosition()).getFeature()
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

		Point p = new Point();
		for (int y = 0; y < board.getHeight(); y++)
			for (int x = 0; x < board.getWidth(); x++) {
				p.setLocation(x, y);
				board.getCell(p).clearVisitsCounter();
			}

		computePaths(board);

		int nAgentsBegin = board.countAgents();
		nTotalAgents += nAgentsBegin;

		// Ilość agentów, którzy osiągnęli swój cel.
		int targetsReached = 0;

		for (int i = 0; i < STEPS; i++) {
			if (i % aviRecorder.getSimFramesPerAviFrame() == 0)
				aviRecorder.recordFrame();

			targetsReached = computeTargetReached();

			if (targetsReached == nAgentsBegin) {
				nAgentSuccesses += nAgentsBegin;
				break;
			}

			try {
				Thread.sleep(GuiState.animationSpeed);
			} catch (InterruptedException e) {
			}

			prepareAgents();

			Map<Agent, Integer> speedPointsLeft = computeMovementPointsLeft();
			moveAgents(speedPointsLeft);
		}

		nAgentSuccesses += targetsReached;

		System.out.println(String.format("Sukcesy agentów:\t %d / %d\t (%d%%)",
				nAgentSuccesses, nTotalAgents, nAgentSuccesses * 100
						/ nTotalAgents));
	}

	private void computePaths(Board board) {
		BlockingQueue<Agent> agentsToCompute = new LinkedBlockingQueue<>(5);
		List<TacticalWorker> threads = new ArrayList<>(NUM_TACTICAL_THREADS);

		for (int i = 0; i < NUM_TACTICAL_THREADS; i++) {
			TacticalWorker t = new TacticalWorker(agentsToCompute, board);
			threads.add(t);
			t.start();
		}

		if (board.countAgents() == 0) {
			Misc.setAgent(new Agent(MovementBehavior.DYNAMIC), new Point(2, 2));
		}

		Point p = new Point();
		for (int y = 0; y < board.getHeight(); y++) {
			for (int x = 0; x < board.getWidth(); x++) {
				p.setLocation(x, y);
				Agent a = board.getCell(p).getAgent();
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
}