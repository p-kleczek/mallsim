package sim.model;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Map;

import sim.model.algo.Ped4;
import sim.model.helpers.Misc;
import sim.model.helpers.MyPoint;
import sim.model.helpers.Vec;

public class Board {

	private Cell[][] grid;

	public Board(Dimension dimension) {
		grid = new Cell[dimension.height][dimension.width];

		for (int y = 0; y < dimension.height; y++)
			for (int x = 0; x < dimension.width; x++)
				grid[y][x] = new Cell(Cell.Type.PASSABLE, Ped4.getInstance());
	}

	/**
	 * Ustawia domyślne ustawienia pól.
	 */
	public void reset() {
		Point p = new Point();
		for (int y = 0; y < getHeight(); y++)
			for (int x = 0; x < getWidth(); x++) {
				p.setLocation(x, y);
				getCell(p).clearVisitsCounter();
				Misc.setAgent(null, p);
			}
	}

	public Board(Cell[][] grid) {
		this.grid = grid;
	}

	/**
	 * Sprawdza, czy punkt zawiera się w obszarze planszy.
	 * 
	 * @param p
	 * @return
	 */
	public boolean isOnBoard(Point p) {
		return p.x >= 0 && p.y >= 0 && p.x < getWidth() && p.y < getHeight();
	}

	public int getWidth() {
		return grid[0].length;
	}

	public int getHeight() {
		return grid.length;
	}

	public void setCell(Point p, Cell cell) {
		grid[p.y][p.x] = cell;
	}

	public Cell getCell(Point p) {
		return grid[p.y][p.x];
	}

	public void computeForceField() {
		// Clear force field.
		Point cellCoord = new Point();

		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				cellCoord.setLocation(x, y);
				Cell cell = getCell(cellCoord);

				cell.setForceValue(0);

				if (cell.getAgent() != null)
					modifyForceField(cell.getAgent(), cell.getAgent()
							.getPosition(), 1);

				cell.flipForceValue();
			}
		}
	}

	/**
	 * Modyfikuje rozkład pola potencjału usuwając lub dodając wartości pola
	 * danego agenta.
	 * 
	 * @param a
	 * @param sign
	 *            <code>1</code> dla dodana siły, <code>-1</code> dla odjęcia
	 */
	public void modifyForceField(Agent a, MyPoint pos, int sign) {
		assert (Math.abs(sign) == 1);

		for (Map.Entry<Vec, Integer> entry : a.getForceField().entrySet()) {
			Vec v = entry.getKey();

			// Pole potencjału przechowywane jest w kierunku N - konieczny
			// obrót.
			switch (a.getDirection()) {
			case N:
				v = v.rotate(0);
				break;
			case E:
				v = v.rotate(1);
				break;
			case S:
				v = v.rotate(2);
				break;
			case W:
				v = v.rotate(3);
				break;
			}

			MyPoint p = pos.add(v);
			if (isOnBoard(p)) {
				getCell(p).changeForce(entry.getValue() * sign);

				if (getCell(p).getForceValue() > 0) {
					System.err.println(p.toString() + " : "
							+ getCell(p).getForceValue());
					throw new AssertionError();
				}

				getCell(p).flipForceValue();
			}
		}
	}

	public int countAgents() {
		int nAgents = 0;
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				Cell c = getCell(new Point(x, y));
				if (c.getAgent() != null) {
					nAgents++;

				}

				// Agent nie może znajdować się na
				// niedostępnym polu.
				assert (!(!c.isPassable() && c.getAgent() != null));
			}
		}

		return nAgents;
	}
}