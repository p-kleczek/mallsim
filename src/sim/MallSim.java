package sim;

import java.awt.EventQueue;

import javax.swing.UIManager;

import sim.control.ResourceManager;
import sim.gui.GUIBoard;
import sim.gui.MallFrame;
import sim.model.Mall;
import sim.util.video.AviRecorder;
import sim.util.video.VideoRecorder;

public class MallSim {

	static Thread simThread = null;
	static MallFrame frame = null;
	private static Simulation simulation = null;

	static boolean isSuspended = false;

	private static VideoRecorder videoRecorder = new AviRecorder();
	
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
		simulation = new Simulation(videoRecorder);
		Mall mall = ResourceManager.loadShoppingMall("ped4-test");
		simulation.setMall(mall);

		frame = new MallFrame(simulation.getMall(), videoRecorder);
		frame.setVisible(true);
		
		videoRecorder.setSource(frame);
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
