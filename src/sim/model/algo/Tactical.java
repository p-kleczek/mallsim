package sim.model.algo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import sim.control.GuiState;
import sim.model.Agent;
import sim.model.Board;
import sim.model.helpers.Rand;

public class Tactical {
	/**
	 * The higher a heuristic factor is, the more likely it is for A* to give up
	 * optimal paths (min = 1).
	 */
	public static final int HEURISTIC_FACTOR = 5;

	public static final int SCORE_FACTOR = 100;
	public static final int MAX_TARGETS = 15;
	public static final int MIN_TARGETS = 1;

	/**
	 * Maximal segment length.
	 */
	public static final int MAX_SEGMENT_SIZE = 9; // =~ 5 m

	/**
	 * If a segment would be smaller than this threshold then all points
	 */
	public static final int PATH_SMOOTHING = 3;
	
	private final static Logger LOGGER = Logger
			.getLogger(Logger.GLOBAL_LOGGER_NAME);

	interface NeighborLookupAlgorithm {
		boolean isNeighbor(Point a, Point b);
	}

	public static class NeighborLookupAlgorithmMoore implements
			NeighborLookupAlgorithm {
		@Override
		public boolean isNeighbor(Point a, Point b) {
			return true;
		}
	}

	public static class NeighborLookupAlgorithmVonNeumann implements
			NeighborLookupAlgorithm {
		@Override
		public boolean isNeighbor(Point a, Point b) {
			int dx = Math.abs(a.x - b.x);
			int dy = Math.abs(a.y - b.y);

			return (dx != dy);
		}
	}

	public static NeighborLookupAlgorithmMoore nlaMoore = new NeighborLookupAlgorithmMoore();
	public static NeighborLookupAlgorithmVonNeumann nlaVonNeumann = new NeighborLookupAlgorithmVonNeumann();

	public static void route(Board board, Agent agent,
			NeighborLookupAlgorithm algorithm) {
		int numTargets = MIN_TARGETS + Rand.nextInt(MAX_TARGETS - MIN_TARGETS);

		LOGGER.info(String.format("Initializing targets for %s...", agent));

		Point[] targets = pickTargets(board, agent, numTargets);

		LOGGER.info(String.format("Picked %d target positions.", targets.length));
		LOGGER.info("Generating paths...");

		computePaths(board, agent, algorithm, targets);

		LOGGER.info("Paths generated!");
		LOGGER.info("Targets initialized!");
	}

	/**
	 * Compute paths between all targets.
	 * 
	 * @param board
	 * @param agent
	 * @param algorithm
	 * @param targets
	 */
	private static void computePaths(Board board, Agent agent,
			NeighborLookupAlgorithm algorithm, Point[] targets) {
		Point last = agent.getPosition();
		for (Point target : targets) {
			List<Point> midpoints = computePath(board, last, target, algorithm);

			for (Point midpoint : midpoints) {
				agent.addTarget(midpoint);
			}

			last = target;
		}

	}

	public static Point[] pickTargets(Board board, Agent agent, int numTargets) {
		List<Point> targets = new ArrayList<Point>();

		// I/O tiles with sufficiently distant to current position.
		targets = new ArrayList<>(board.getIoPoints());
		for (Iterator<Point> it = targets.iterator(); it.hasNext();) {
			Point p = it.next();
			if (p.distance(agent.getPosition()) < 10)
				it.remove();
		}

		// Draw one target tile.
		Point target = targets.get(Rand.nextInt(targets.size()));
		targets = new ArrayList<>();
		targets.add(target);

		// Random.
		// while (targets.size() < numTargets) {
		// Point p = new Point(Rand.nextInt(board.getWidth()),
		// Rand.nextInt(board.getHeight()));
		//
		// if (board.getCell(p) != Cell.WALL) {
		// targets.add(p);
		// }
		// }

		// Sort by overal distance:
		Point[] targetArray = targets.toArray(new Point[0]);

		Arrays.sort(targetArray, new PointComparator(agent.getPosition()));

		for (int i = 1; i < targetArray.length; ++i) {
			Arrays.sort(targetArray, i, targetArray.length,
					new PointComparator(targetArray[i - 1]));
		}

		return targetArray;
	}

	/**
	 * Compute path between two points.
	 * 
	 * @param board
	 * @param start
	 * @param targetPoint
	 * @param algorithm
	 * @return
	 */
	public static List<Point> computePath(Board board, Point start,
			Point targetPoint, NeighborLookupAlgorithm algorithm) {
		int width = board.getWidth();
		int height = board.getHeight();

		BitSet closed = new BitSet(width * height);
		PriorityQueue<Node> open = new PriorityQueue<Node>(100,
				new NodeComparator());

		Node node = new Node(); // Only for lookups.
		Node current = null;

		Node startNode = new Node(start, 0, heuristicCostEstimate(board, start,
				targetPoint));
		open.add(startNode);

		while (!open.isEmpty()) {
			current = open.poll();

			Point currentPoint = current.point;
			if (currentPoint.equals(targetPoint)) {
				break;
			}

			closed.set(currentPoint.y * width + currentPoint.x);

			for (Point neighbour : getNeighbours(board, currentPoint, algorithm)) {
				node.point = neighbour;

				if (closed.get(neighbour.y * width + neighbour.x)) {
					continue;
				}

				int score = current.score
						+ getScoreDelta(currentPoint, neighbour);
				int estimate = score
						+ heuristicCostEstimate(board, neighbour,
								targetPoint);

				Node n = null;
				if (open.contains(node)) {
					if (score > node.score)
						continue;

					for (Iterator<Node> iter = open.iterator(); iter.hasNext()
							&& !n.equals(node);) {
						n = iter.next();
					}

					assert n != null;

					n.estimate = estimate;
				} else {
					n = new Node(neighbour);
					n.estimate = estimate;

					open.add(n);
				}

				n.previousNode = current;
				n.score = score;
			}
		}

		// Reconstruct path.
		List<Point> allpoints = new ArrayList<Point>();
		if (current != null && current.point.equals(targetPoint)) {
			while (current != null) {
				allpoints.add(0, current.point);
				current = current.previousNode;
			}

			allpoints = selectMidpoints(board, allpoints);
		}

		return allpoints;
	}

	private static List<Point> selectMidpoints(Board board, List<Point> points) {
		int size = points.size();

		if (size > MAX_SEGMENT_SIZE) {
			List<Point> midpoints = new ArrayList<Point>();

			int numSegments = size / MAX_SEGMENT_SIZE;

			for (int i = 0; i < numSegments; i++) {
				int startIndex = i * MAX_SEGMENT_SIZE;
				int endIndex = Math.min((i + 1) * MAX_SEGMENT_SIZE, size - 1);

				List<Point> segment = selectMidpoints(board, points,
						startIndex, endIndex);

				// For the first segment its starting point is the Agent
				// position, which is not a part of the path.
				// For all other segments their starting points are the same as
				// the ending points of the previous segments, so we can safely
				// remove them.

				if (segment.size() > 1)
					segment.remove(0);
				midpoints.addAll(segment);
			}

			return midpoints;
		} else {
			return selectMidpoints(board, points, 0, size - 1);
		}
	}

	private static List<Point> selectMidpoints(Board board, List<Point> points,
			int start, int end) {
		List<Point> result = new ArrayList<Point>();

		Point startPoint = points.get(start);
		Point endPoint = points.get(end);

		if (end == start) {
			result.add(startPoint);
		}

		if (end - start < PATH_SMOOTHING) {
			for (int i = start; i <= end; ++i) {
				result.add(points.get(i));
			}
		} else if (isTargetVisible(board, startPoint, endPoint)) {
			result.add(startPoint);
			result.add(endPoint);
		} else {
			int middle = (start + end) / 2;

			List<Point> a = selectMidpoints(board, points, start, middle);
			List<Point> b = selectMidpoints(board, points, middle, end);

			b.removeAll(a); // remove duplicants
			result.addAll(a);
			result.addAll(b);
		}

		return result;
	}

	/**
	 * Checks whether point B is visible from point A.
	 * 
	 * @param board
	 * @param startPoint
	 * @param endPoint
	 * @return
	 */
	private static boolean isTargetVisible(Board board, Point startPoint,
			Point endPoint) {
		if (startPoint.equals(endPoint))
			return true;

		Point p = new Point();

		double x = startPoint.x;
		double y = startPoint.y;

		double dist = Point.distance(startPoint.x, startPoint.y, endPoint.x,
				endPoint.y);
		double dx = (endPoint.x - startPoint.x) / dist;
		double dy = (endPoint.y - startPoint.y) / dist;

		while (!p.equals(endPoint)) {
			p.setLocation(x, y);

			if (!board.isOnBoard(p) || !board.getCell(p).isPassable())
				return false;

			x += dx;
			y += dy;
		}

		return true;
	}

	private static int heuristicCostEstimate(Board board, Point p, Point target) {
		// XXX: tu nie powinno być metryki miejskiej?
		int score = (int) (SCORE_FACTOR * HEURISTIC_FACTOR * p.distance(target));

		MallFeature mf = board.getCell(p).getFeature();

		return (mf != null) ? mf.modifyHeuristicEstimate(score) : score;
	}

	private static List<Point> getNeighbours(Board board, Point point,
			NeighborLookupAlgorithm algorithm) {
		ArrayList<Point> neighbours = new ArrayList<Point>();

		Point p = new Point();
		for (int dy = -1; dy <= 1; ++dy) {
			for (int dx = -1; dx <= +1; ++dx) {
				p.setLocation(point.x + dx, point.y + dy);

				if (!algorithm.isNeighbor(point, p) || p.equals(point)
						|| !board.isOnBoard(p)
						|| !board.getCell(p).isPassable())
					continue;

				neighbours.add(new Point(p));
			}
		}

		return neighbours;
	}

	private static int getScoreDelta(Point a, Point b) {
		return SCORE_FACTOR;
	}

	private static class Node {
		public Point point = null;
		public int score = 0;
		public int estimate = 0;
		public Node previousNode = null;

		public Node() {
			this(null);
		}

		public Node(Point point) {
			this(point, 0, 0);
		}

		public Node(Point point, int score, int estimate) {
			this.point = point;
			this.score = score;
			this.estimate = estimate;
		}

		public boolean equals(Object o) {
			if (o instanceof Node) {
				Node n = (Node) o;
				return this.point.equals(n.point);
			}

			return false;
		}
	}

	/**
	 * Sort by estimate in a decreasing order.
	 * 
	 * @author Kajetan Rzepecki
	 * 
	 */
	private static class NodeComparator implements Comparator<Node> {
		public boolean equals(Object o) {
			if (o instanceof NodeComparator) {
				return this == o;
			}
			return false;
		}

		@Override
		public int compare(Node n0, Node n1) {
			return n0.estimate - n1.estimate;
		}
	}

	private static class PointComparator implements Comparator<Point> {
		private Point p = null;

		public PointComparator(Point p) {
			this.p = p;
		}

		@Override
		public int compare(Point pa, Point pb) {
			// XXX: co to za stała?
			if (pa.distance(pb) < 20)
				return 0;

			return (int) (p.distanceSq(pa) - p.distanceSq(pb));
		}

		public boolean equals(Object o) {
			if (o instanceof NodeComparator) {
				return this == o;
			}
			return false;
		}
	}
}