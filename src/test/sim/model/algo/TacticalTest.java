package test.sim.model.algo;

import static org.junit.Assert.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import sim.model.algo.Tactical;

public class TacticalTest {
	private static List<Point> grid = new ArrayList<>();
	
	@BeforeClass
	public static void setUpBeforeClass() {
		for (int x = 0; x < 3; x++)
			for (int y = 0; y < 3; y++)
				grid.add(new Point(x, y));
	}
	
	
	@Test
	public void neighborTestMoore() {
		Point center = new Point(1, 1);
		List<Point> neighbors = new ArrayList<>();
		
		for (Point p : grid)
			if (Tactical.nlaMoore.isNeighbor(center, p))
				neighbors.add(p);
		
		assertEquals(neighbors.size(), 9); // bo środek też..
	}

	@Test
	public void neighborTestVonNeumann() {
		Point center = new Point(1, 1);
		List<Point> neighbors = new ArrayList<>();
		
		for (Point p : grid)
			if (Tactical.nlaVonNeumann.isNeighbor(center, p))
				neighbors.add(p);
		
		assertTrue(neighbors.contains(new Point(1, 0)));	// N
		assertTrue(neighbors.contains(new Point(2, 1)));	// E
		assertTrue(neighbors.contains(new Point(1, 2)));	// S
		assertTrue(neighbors.contains(new Point(0, 1)));	// W
	}

}
