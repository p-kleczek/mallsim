package sim;

import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import sim.control.GuiState;
import sim.control.ResourceManager;
import sim.gui.GUIBoard;
import sim.gui.MallFrame;
import sim.model.Mall;
import sim.util.video.AviRecorder;
import sim.util.video.VideoRecorder;

public class MallSim {

	public static MallFrame frame = null;

	private static Simulation simulation = null;
	private static VideoRecorder videoRecorder = null;

	static boolean isSuspended = false;

	static Thread simThread = null;
	
	private final static Logger LOGGER = Logger
			.getLogger(Logger.GLOBAL_LOGGER_NAME);

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

		LOGGER.setLevel(Level.SEVERE);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				if (simulation != null)
					simulation.finish();
			}

		});

		EventQueue.invokeLater(new Runnable() {

			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager
							.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				frame.setVisible(true);
			}
		});
	}

	public static void runSimulation() {
		Mall mall = ResourceManager.loadShoppingMall(GuiState.currentResourcePath);
		simulation.setMall(mall);
		simulation.configureLogFile();

		frame.setMall(simulation.getMall());
		frame.revalidate();
		frame.repaint();

		videoRecorder.setSource(frame);

		frame.getSummaryTable().clear();

		simulation.addObserver(frame.getBoard());

		if (simThread != null) {
			simThread.stop();
		}

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
}
