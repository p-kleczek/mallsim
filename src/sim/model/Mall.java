package sim.model;

import java.awt.Dimension;

public class Mall {
	private Board board;

	// Default ctor
	public Mall() {
		board = new Board(new Dimension(15, 10));
	}

	public void setBoard(Board b) {
		board = b;
	}

	public Board getBoard() {
		return board;
	}

	public void reset() {
		board.reset();
	}
}
