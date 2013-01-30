package sim;

import java.awt.EventQueue;

import javax.swing.UIManager;

import sim.control.ResourceManager;
import sim.gui.GUIBoard;
import sim.gui.MallFrame;
import sim.model.Board;
import sim.model.Mall;
import sim.util.AviRecorder;

public class MallSim {

	static Thread simThread = null;
	static MallFrame frame = null;

	static boolean isSuspended = false;

	private static AviRecorder aviRecorder = new AviRecorder(frame);

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {

			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager
							.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					e.printStackTrace();
				}

				ResourceManager.loadShoppingMall("./data/malls/gk0.bmp",
						"./data/malls/gk0map.bmp");

				if (frame == null) {
					frame = new MallFrame(Mall.getInstance(), aviRecorder);
				}

				frame.setVisible(true);

				runAlgoTest();
			}
		});
	}

	/**
	 * Testuje działanie algotymów ruchu.
	 */
	public static void runAlgoTest() {
		Mall.getInstance().reset();
		Simulation loop = new Simulation(Mall.getInstance(), aviRecorder);
		loop.addObserver(frame.getBoard());
		Board b = Mall.getInstance().getBoard();
		ResourceManager.randomize(Mall.getInstance().getBoard(),
				b.getDimension().height * b.getDimension().width / 50);

		if (simThread != null)
			simThread.stop();
		simThread = new Thread(loop);
		simThread.start();

		if (isSuspended)
			simThread.suspend();
	}


	synchronized public static void setThreadState(boolean _isSuspended) {
		isSuspended = _isSuspended;

		if (isSuspended)
			simThread.suspend();
		else
			simThread.resume();
	}

	synchronized public static boolean getThreadState() {
		return isSuspended;
	}

	// public static Thread getThread() {
	// return simThread;
	// }

	public static GUIBoard getGUIBoard() {
		return (frame != null) ? frame.getBoard() : null;
	}
}
