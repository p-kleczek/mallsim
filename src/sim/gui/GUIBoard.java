package sim.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;

import sim.control.GuiState;
import sim.control.GuiState.BackgroundPolicy;
import sim.control.GuiState.DrawTargetLinePolicy;
import sim.gui.helpers.Helpers;
import sim.model.Agent;
import sim.model.Board;
import sim.model.Cell;
import sim.model.algo.MallFeature;
import sim.model.algo.Ped4;

@SuppressWarnings("serial")
public class GUIBoard extends JComponent implements Observer {

	private Board board;
	private final GUIBoard instance;

	/**
	 * If the cell size is smaller than this value, borders will not be drawn.
	 */
	private static final int CELL_SIZE_BORDER_THRESHOLD = 10;

	private int cellSize = 14;
	private int agentSize = 16;

	private int resizeDelta = 1; // Used for dynamic resizing.

	int maxVisits;

	public GUIBoard(Board board) {
		instance = this;
		this.board = board;
		setDoubleBuffered(true);

		initialize();
	}

	private void initialize() {
		addMouseListener(new MyMouseListener());
		addMouseWheelListener(new MyMouseWheelListener());
		setBackground(Color.WHITE);
		setOpaque(true);

		setPreferredSize(new Dimension(board.getWidth() * cellSize + 1,
				board.getHeight() * cellSize + 1));
	}

	protected void paintComponent(Graphics g) {
		if (isOpaque()) {
			g.setColor(getBackground());
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
		}

		g.setColor(Color.GRAY);
		drawNetting(g, cellSize);

		if (GuiState.targetLinePolicy == DrawTargetLinePolicy.SELECTION_ROUTE) {
			Agent a = GuiState.getSelectedAgent();
			if (a != null)
				drawRoute(g, a);
		}
	}

	private void drawNetting(Graphics g, int gridSpace) {
		Insets insets = getInsets();
		int firstX = insets.left;
		int firstY = insets.top;
		int lastX = this.getWidth() - insets.right;
		int lastY = this.getHeight() - insets.bottom;

		int x, y;

		if (cellSize > CELL_SIZE_BORDER_THRESHOLD) {
			x = firstX;
			while (x < lastX) {
				g.drawLine(x, firstY, x, lastY);
				x += gridSpace;
			}

			y = firstY;
			while (y < lastY) {
				g.drawLine(firstX, y, lastX, y);
				y += gridSpace;
			}
		}

		Map<Point, Point> targets = new HashMap<Point, Point>();

		if (GuiState.backgroundPolicy == BackgroundPolicy.VISITS)
			maxVisits = computeMaxVisits();

		Point p = new Point();
		for (y = 0; y < board.getHeight(); y++) {
			for (x = 0; x < board.getWidth(); x++) {
				p.setLocation(x, y);
				Cell c = board.getCell(p);

				g.setColor(computeCellColor(c));

				if (cellSize > CELL_SIZE_BORDER_THRESHOLD)
					g.fillRect((x * cellSize) + 1, (y * cellSize) + 1,
							(cellSize - 1), (cellSize - 1));
				else
					g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);

				Agent a = c.getAgent();

				if (a != null) {
					drawAgent(g, a, x, y);

					if (GuiState.targetLinePolicy != DrawTargetLinePolicy.NONE) {
						if (a.getTargetCount() > 0) {
							if (GuiState.targetLinePolicy == DrawTargetLinePolicy.ALL
									|| GuiState.targetLinePolicy == DrawTargetLinePolicy.SELECTION
									&& a == GuiState.getSelectedAgent()) {
								targets.put(new Point(p), a.getTarget());
							}
						}
					}
				}
			}
		}

		// Draw target lines.
		final float TARGET_VECTOR_WIDTH = 3f;
		g.setColor(Color.CYAN);
		Graphics2D g2d = (Graphics2D) g;
		Stroke s = g2d.getStroke();
		g2d.setStroke(new BasicStroke(TARGET_VECTOR_WIDTH)); // set stroke
																// width of 10
		for (Map.Entry<Point, Point> entry : targets.entrySet()) {
			int x1 = entry.getKey().x * cellSize + cellSize / 2;
			int y1 = entry.getKey().y * cellSize + cellSize / 2;
			int x2 = entry.getValue().x * cellSize + cellSize / 2;
			int y2 = entry.getValue().y * cellSize + cellSize / 2;

			g.drawLine(x1, y1, x2, y2);
		}
		g2d.setStroke(s);
	}

	private int computeMaxVisits() {
		Point p = new Point();
		int vmax = 0;
		for (int y = 0; y < board.getHeight(); y++) {
			for (int x = 0; x < board.getWidth(); x++) {
				p.setLocation(x, y);
				vmax = Math.max(vmax, board.getCell(p).getVisitsCounter());
			}
		}

		return vmax;
	}

	private Color computeCellColor(Cell cell) {

		Color color = null;
		switch (cell.getType()) {
		case PASSABLE:
			switch (GuiState.backgroundPolicy) {
			case SOCIAL_FIELD:
				color = getPassableColor(cell);
				break;
			case VISITS:
				if (cell.getVisitsCounter() == 0)
					color = Color.WHITE;
				else
					color = new Color(1.0f, 1.0f - Math.min(1.0f,
							(float) cell.getVisitsCounter() / maxVisits), 0.0f);
				break;
			case MOVEMENT_ALGORITHM:
				if (cell.getAlgorithm() == Ped4.getInstance()) {
					color = new Color(0xcccccc);
				} else {
					color = Color.WHITE;
				}
				break;
			case FEATURES:
				MallFeature feature = cell.getFeature();
				if (feature != null) {
					color = new Color(feature.getPixelValue());
				} else {
					color = Color.WHITE;
				}
				break;
			case NONE:
				color = Color.WHITE;
				break;
			case LANES:
				switch (cell.getLaneDirection()) {
				case EAST:
					color = Color.YELLOW;
					break;
				case EMPTY:
					color = Color.WHITE;
					break;
				case NONE:
					color = Color.LIGHT_GRAY;
					break;
				case WEST:
					color = Color.GREEN;
					break;
				default:
					break;

				}
				break;
			default:
				break;
			}
			break;
		case BLOCKED:
			color = Color.BLACK;
			break;
		}

		return color;
	}

	private Color getPassableColor(Cell c) {
		int forceValue = c.getForceValue4Rendering();

		if (forceValue == 0)
			return Color.WHITE;

		int maxSummedForceValue = 8 * Agent.FORCE_VALUE_MAX;
		int coef = 255 * forceValue / maxSummedForceValue;

		// Ujemny współczynnik powstaje np. gdy ludzie długo chodzą w kółko.
		try {
			return (coef > 0) ? new Color(coef, 255 - coef, 0) : new Color(
					-coef, 0, 255 + coef);
		} catch (Exception e) {
			// FIXME Bugs like crazy.
			System.err.println("Niewłaściwe wartości kolorów: ");
			System.err
					.println(String.format("%d, %d, %d", coef, 255 - coef, 0));
			System.err.println(String
					.format("%d, %d, %d", -coef, 0, 255 + coef));
			return null;
		}
	}

	private void drawRoute(Graphics g, Agent a) {
		// FIXME Throws java.util.ConcurrentModificationException sometimes.

		final float TARGET_VECTOR_WIDTH = 3f;
		Graphics2D g2d = (Graphics2D) g;
		Stroke s = g2d.getStroke();
		g2d.setStroke(new BasicStroke(TARGET_VECTOR_WIDTH));

		Point last = a.getPosition();
		boolean toggle = true;

		for (Point p : a.getRoute()) {
			g.setColor(toggle ? Color.CYAN : Color.GREEN);

			int x1 = last.x * cellSize + cellSize / 2;
			int y1 = last.y * cellSize + cellSize / 2;
			int x2 = p.x * cellSize + cellSize / 2;
			int y2 = p.y * cellSize + cellSize / 2;

			g.drawLine(x1, y1, x2, y2);

			last = p;
			toggle = !toggle;
		}

		g2d.setStroke(s);
	}

	// Convinience method for the soon to be lengthy Agent drawing code.
	private void drawAgent(Graphics g, Agent a, int x, int y) {
		final int DIRECTION_MARKER_LENGTH = 10;

		assert a != null;
		assert g != null;

		Color torsoColor = (a == GuiState.getSelectedAgent()) ? Color.RED
				: Color.BLUE;
		g.setColor(torsoColor);

		int agentH = 0;
		int agentW = 0;
		int agentHeadSize = agentSize / 3;

		switch (a.getDirection()) {
		case N:
		case S:
			agentH = agentSize / 2;
			agentW = agentSize;
			break;
		case E:
		case W:
			agentH = agentSize;
			agentW = agentSize / 2;
			break;
		}

		int agentX = x * cellSize + cellSize / 2;
		int agentY = y * cellSize + cellSize / 2;

		g.fillOval(agentX - agentW / 2, agentY - agentH / 2, agentW, agentH);

		g.setColor(Color.GREEN);
		g.drawLine(agentX, agentY, agentX + a.getDirection().getVec().x
				* DIRECTION_MARKER_LENGTH, agentY + a.getDirection().getVec().y
				* DIRECTION_MARKER_LENGTH);

		g.setColor(Color.YELLOW);
		g.fillOval(agentX - agentHeadSize / 2, agentY - agentHeadSize / 2,
				agentHeadSize, agentHeadSize);
	}

	private class MyMouseListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			int x = e.getX() / cellSize;
			int y = e.getY() / cellSize;

			Agent a = board.getCell(new Point(x, y)).getAgent();

			Component c = instance;
			while (!(c instanceof MallFrame))
				c = c.getParent();

			GuiState.setSelectedAgent(a, (MallFrame) c);
			repaint();
		}

	}

	private class MyMouseWheelListener implements MouseWheelListener {

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			zoom(e.getWheelRotation());
		}

	}
	
	public void zoom(int step) {
		cellSize = Helpers.clamp(cellSize + resizeDelta * step, 3,
				25);
		agentSize = Helpers.clamp(agentSize + resizeDelta * step,
				2, 27);

		Dimension d = new Dimension(board.getWidth() * cellSize + 1,
				board.getHeight() * cellSize + 1);

		setPreferredSize(d);

		repaint();
		revalidate();
	}

	@Override
	public void update(Observable o, Object arg) {
		repaint();
	}

	public void setBoard(Board board) {
		this.board = board;
	}

}
