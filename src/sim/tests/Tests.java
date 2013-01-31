package sim.tests;

import java.awt.Dimension;
import java.awt.Point;

import sim.model.Agent;
import sim.model.Board;
import sim.model.Cell;
import sim.model.Agent.MovementBehavior;
import sim.model.algo.Ped4;
import sim.model.algo.SocialForce;
import sim.model.algo.Tactical;
import sim.model.helpers.Direction;
import sim.model.helpers.Misc;
import sim.model.helpers.Rand;

public class Tests {
	public static void testSocialForce(Board b) {
		for (int y = 0; y < b.getDimension().height; y++)
			for (int x = 0; x < b.getDimension().width; x++) {
				Point p = new Point(x, y);
				Misc.setAgent(null, p);
				b.getCell(p).setAlgorithm(SocialForce.getInstance());
			}

		// testSocialForceIndividual(b);
		testSocialForceMassive(b);
	}

	public static void testSocialForceIndividual(Board b) {
		final int TARGET_BEHIND = 1;
		final int WALK_AROUND = 2;

		int mode = TARGET_BEHIND;

		if (mode == TARGET_BEHIND) {
			Agent a1 = new Agent(MovementBehavior.DYNAMIC);
			a1.addTarget(new Point(3, 6));
			a1.setInitialDistanceToTarget(new Point(5, 6).distance(new Point(3,
					6)));
			Misc.setAgent(a1, new Point(5, 6));
		} else if (mode == WALK_AROUND) {
			Agent a1 = new Agent(MovementBehavior.DYNAMIC);
			a1.addTarget(new Point(12, 3));
			a1.setInitialDistanceToTarget(new Point(2, 6).distance(new Point(
					12, 3)));
			Misc.setAgent(a1, new Point(2, 6));

			Agent a2 = new Agent(MovementBehavior.DYNAMIC);
			a2.addTarget(new Point(7, 5));
			a2.setInitialDistanceToTarget(new Point(7, 5).distance(new Point(7,
					5)));
			Misc.setAgent(a2, new Point(7, 5));
		}
	}

	public static void testSocialForceMassive(Board b) {
		final int N_AGENTS = 10;

		Dimension d = b.getDimension();
		for (int i = 0; i < N_AGENTS; i++) {
			Point p = new Point(Rand.nextInt(d.width), Rand.nextInt(d.height));
			Cell c = b.getCell(p);

			if (c != Cell.WALL) {
				Agent a = new Agent(MovementBehavior.DYNAMIC);
				a.addTarget(new Point(Rand.nextInt(d.width), Rand
						.nextInt(d.height)));
				a.setInitialDistanceToTarget(p.distance(a.getTarget()));
				a.setDirection(Direction.values()[Rand.nextInt(Direction
						.values().length)]);
				Misc.setAgent(a, p);
			}
		}
	}

	public static void testPed4(Board b) {
		for (int y = 0; y < b.getDimension().height; y++)
			for (int x = 0; x < b.getDimension().width; x++) {
				Point p = new Point(x, y);
				Misc.setAgent(null, p);
				b.getCell(p).setAlgorithm(Ped4.getInstance());
			}

		// testPed4Individual(b);
		testPed4Massive(b);
	}

	public static void testPed4Individual(Board b) {
		for (int y = 0; y < b.getDimension().height; y++)
			for (int x = 0; x < b.getDimension().width; x++)
				Misc.setAgent(null, new Point(x, y));

		for (int y = 3; y < 8; y++)
			for (int x = 5; x < 10; x++)
				;
		// b.setCell(new Point(x, y), Cell.WALL);

		Agent a = new Agent(MovementBehavior.DYNAMIC);
		a.addTarget(new Point(8, 5));
		a.setInitialDistanceToTarget(new Point(0, 0).distance(new Point(8, 5)));
		a.addTarget(new Point(4, 1));
		Misc.setAgent(a, new Point(0, 0));
	}

	public static void testPed4Massive(Board b) {
		final int N_AGENTS = 20;

		Dimension d = b.getDimension();
		for (int i = 0; i < N_AGENTS; i++) {
			Point p = new Point(Rand.nextInt(d.width), Rand.nextInt(d.height));

			if (b.getCell(p) != Cell.WALL) {
				Agent a = new Agent(MovementBehavior.DYNAMIC);
				a.addTarget(new Point(Rand.nextInt(d.width), Rand
						.nextInt(d.height)));
				a.setInitialDistanceToTarget(p.distance(a.getTarget()));
				a.setDirection(Direction.values()[Rand.nextInt(Direction
						.values().length)]);
				Misc.setAgent(a, p);
			}
		}
	}

}
