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
	private static Simulation simulation = null;

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

				prepare();
				runSimulation();
			}
		});
	}

	private static void prepare() {
		simulation = new Simulation(aviRecorder);
		Mall mall = ResourceManager.loadShoppingMall("ped4-test");
		simulation.setMall(mall);

		frame = new MallFrame(simulation.getMall(), aviRecorder);
		frame.setVisible(true);
	}

	/**
	 * Testuje działanie algotymów ruchu.
	 */
	public static void runSimulation() {
		simulation.addObserver(frame.getBoard());

		if (simThread != null)
			simThread.stop();
		simThread = new Thread(simulation);
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
