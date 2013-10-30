package sim.util.video;

/**
 * Zapisuje sekwencję zrzutów ekranu obszaru roboczego do pliku wideo (w
 * formacie AVI).
 * 
 * @author Pawel
 * 
 */
public abstract class VideoRecorderImpl implements VideoRecorder {

	/**
	 * Liczba klatek symulacji na jedną klatkę animacji (zapisywanej do pliku
	 * AVI).
	 */
	private int simFramesPerAviFrame = 1;
	private String outputFilename = "out.avi";

	public VideoRecorderImpl() {
	}

	public int getSimFramesPerAviFrame() {
		return simFramesPerAviFrame;
	}

	public void setSimFramesPerAviFrame(int simFramesPerAviFrame) {
		this.simFramesPerAviFrame = simFramesPerAviFrame;
	}

	@Override
	public void setOutputFilename(String filename) {
		outputFilename = new String(filename);
	}

	String getOutputFilename() {
		return outputFilename;
	}
}
