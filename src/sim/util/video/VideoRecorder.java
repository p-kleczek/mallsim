package sim.util.video;

import java.awt.AWTException;
import java.io.IOException;

import sim.gui.MallFrame;

public interface VideoRecorder {
	void setSource(MallFrame frame);
	
	void setOutputFilename(String filename);

	int getSimFramesPerAviFrame();

	void setSimFramesPerAviFrame(int simFramesPerAviFrame);

	void prepare() throws IOException, AWTException;

	void finish() throws IOException, AWTException;

	void recordFrame();
}
