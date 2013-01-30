package sim;

import java.awt.Point;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.WeakHashMap;

import sim.control.GuiState;
import sim.model.Agent;
import sim.model.Board;
import sim.model.Mall;
import sim.model.helpers.Rand;
import sim.tests.Tests;
import sim.util.AviRecorder;

public class Simulation extends Observable implements Runnable {

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
	 * @return ilość agentów, którzy dotarli do celu
	 */
	private int computeTargetReached() {
		int targetsReached = 0;

		// Sprawdzanie, czy cel został osiągnięty.
		for (int y1 = 0; y1 < board.getDimension().height; y1++) {
			for (int x1 = 0; x1 < board.getDimension().width; x1++) {
				Point curr = new Point(x1, y1);
				Agent a = board.getCell(curr).getAgent();

				if (a != null && a.getTargetCount() > 0) {
					if (a.getTarget().equals(curr)) {
						a.reachTarget();
						if (a.getTargetCount() > 0)
							a.setInitialDistanceToTarget(curr.distanceSq(a
									.getTarget()));
					} else {
						final double maxDistanceFromTarget = 2;
						double dist = a.getTarget().distance(curr);
						if (dist < maxDistanceFromTarget) {
							// TODO: metoda probabilistyczna
							if (Rand.nextDouble() < 1 / (dist * dist)) {
								a.reachTarget();

								if (a.getTargetCount() > 0)
									a.setInitialDistanceToTarget(curr
											.distanceSq(a.getTarget()));
							}
						}
					}
				}

				if (a != null && a.getTargetCount() == 0)
					targetsReached++;
			}
		}

		return targetsReached;
	}

	private void prepareAgents() {
		// Movement
		for (int y = 0; y < board.getDimension().height; y++) {
			for (int x = 0; x < board.getDimension().width; x++) {
				Point p = new Point(x, y);
				Agent a = board.getCell(p).getAgent();

				if (a != null && a.getTargetCount() > 0) {
					if (!a.getTarget().equals(p))
						board.getCell(p).getAlgorithm().prepare(a);
				}

			}
		}

		this.setChanged();
		this.notifyObservers();
	}

	private Map<Agent, Integer> computeMovementPointsLeft() {
		// Obliczenie ilości pozostałych punktów ruchu dla agentów.
		Map<Agent, Integer> speedPointsLeft = new WeakHashMap<Agent, Integer>();
		for (int y = 0; y < board.getDimension().height; y++) {
			for (int x = 0; x < board.getDimension().width; x++) {
				Point p = new Point(x, y);
				Agent a = board.getCell(p).getAgent();

				if (a != null)
					speedPointsLeft.put(a, a.getvMax());
			}
		}

		return speedPointsLeft;
	}

	private void moveAgents(Map<Agent, Integer> speedPointsLeft) {
		Point p = new Point();
		Set<Agent> moved = new HashSet<Agent>();
		for (int step = 0; step < Agent.V_MAX; step++) {
			moved.clear();
			for (int y = 0; y < board.getDimension().height; y++) {
				for (int x = 0; x < board.getDimension().width; x++) {
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
		final int LOOPS = 10;
		final int STEPS = 5000;

		// Liczba poprawnie zakończonych iteracji (wszystkie cele
		// osiągnięte).
		int nSuccesses = 0;

		int nTotalAgents = 0;
		int nAgentSuccesses = 0;

		System.out.println(board.countAgents());

		loop: for (int lp = 0; lp < LOOPS; lp++) {

			// testSocialForce(board);
			// testPed4(board);

			Point p = new Point();
			for (int y = 0; y < board.getDimension().height; y++)
				for (int x = 0; x < board.getDimension().width; x++) {
					p.setLocation(x, y);
					board.getCell(p).clearVisitsCounter();
				}

			Tests.testTactical(board);

			int nAgentsBegin = board.countAgents();
			nTotalAgents += nAgentsBegin;

			// System.out.println("begin = " + nAgentsBegin);

			// Ilość agentów, którzy osiągnęli swój cel.
			int targetsReached = 0;

			for (int i = 0; i < STEPS; i++) {
				if (i % aviRecorder.getSimFramesPerAviFrame() == 0)
					aviRecorder.recordFrame();

				targetsReached = computeTargetReached();

				if (targetsReached == nAgentsBegin) {
					nSuccesses++;
					nAgentSuccesses += nAgentsBegin;
					continue loop;
				}

				try {
					Thread.sleep(GuiState.animationSpeed);
				} catch (InterruptedException e) {
				}

				prepareAgents();

				Map<Agent, Integer> speedPointsLeft = computeMovementPointsLeft();
				moveAgents(speedPointsLeft);

				// assert (nAgentsBegin == board.countAgents());
				// System.out.println(String.format("i[%d] = %d", i,
				// board.countAgents()));
			}

			// System.out.println("end = " + board.countAgents());

			nAgentSuccesses += targetsReached;
		}

		System.out.println(String.format(
				"Sukcesy pętli:  \t %d / %d\t (%d%%)", nSuccesses, LOOPS,
				nSuccesses * 100 / LOOPS));
		System.out.println(String.format(
				"Sukcesy agentów:\t %d / %d\t (%d%%)", nAgentSuccesses,
				nTotalAgents, nAgentSuccesses * 100 / nTotalAgents));
	}
}