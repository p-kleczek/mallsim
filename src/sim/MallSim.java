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

	public static MallFrame frame = null;
	private static String mallName = "./data/malls/sd-test_map.bmp";

	private static Simulation simulation = null;
	private static VideoRecorder videoRecorder = null;

	static boolean isSuspended = false;
	
	static Thread simThread = null;

	// Uwaga! Ważna jest kolejnosc!
	static {
		videoRecorder = new AviRecorder();
		frame = new MallFrame(videoRecorder);
		simulation = new Simulation(videoRecorder);
		
	}


	public static MallFrame getFrame() {
		return frame;
	}

	public static GUIBoard getGUIBoard() {
		return (frame != null) ? frame.getBoard() : null;
	}

	synchronized public static boolean getThreadState() {
		return isSuspended;
	}

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

				runSimulation();
			}
		});
	}

	public static void runSimulation() {
		Mall mall = ResourceManager.loadShoppingMall(mallName);
		simulation.setMall(mall);

		frame.setMall(simulation.getMall());
		frame.setVisible(true);

		videoRecorder.setSource(frame);
		
		frame.getSummaryTable().clear();

		
		simulation.addObserver(frame.getBoard());

		if (simThread != null)
			simThread.stop();
		
		simThread = new Thread(simulation);
		
		simThread.start();

		if (isSuspended)
			simThread.suspend();
	}

	public static void setMallName(String mallName) {
		MallSim.mallName = mallName;
	}

	// public static Thread getThread() {
	// return simThread;
	// }

	synchronized public static void setThreadState(boolean _isSuspended) {
		isSuspended = _isSuspended;

		if (isSuspended)
			simThread.suspend();
		else
			simThread.resume();
	}
}
