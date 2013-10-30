package sim.control;

import java.util.concurrent.BlockingQueue;

import sim.model.Agent;
import sim.model.Board;
import sim.model.algo.Tactical;

public class TacticalWorker extends Thread {

	private final BlockingQueue<Agent> queue;
	private boolean isStopped = false;
	private final Board board;

	public TacticalWorker(BlockingQueue<Agent> queue, Board board) {
		super();
		this.queue = queue;
		this.board = board;
	}

	@Override
	public void run() {
		while (true) {
			Agent a = null;
			try {
				a = queue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println(this.getId() + "  [" + queue.size() + "]");

			if (a != null) {
				a.clearTargets();
				Tactical.route(board, a, Tactical.nlaMoore);
				
				assert a.getTargetCount() > 0;
			}

			synchronized (this) {
				if (isStopped)
					break;
			}

		}
	}

	public synchronized void setStopped() {
		this.isStopped = true;
	}

}
