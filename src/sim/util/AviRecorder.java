package sim.util;

import static org.monte.media.FormatKeys.EncodingKey;
import static org.monte.media.FormatKeys.FrameRateKey;
import static org.monte.media.FormatKeys.MediaTypeKey;
import static org.monte.media.VideoFormatKeys.DepthKey;
import static org.monte.media.VideoFormatKeys.ENCODING_AVI_MJPG;
import static org.monte.media.VideoFormatKeys.HeightKey;
import static org.monte.media.VideoFormatKeys.QualityKey;
import static org.monte.media.VideoFormatKeys.WidthKey;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.monte.media.Format;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.avi.AVIWriter;
import org.monte.media.math.Rational;

import sim.gui.MallFrame;

public class AviRecorder {

	private MallFrame frame;

	/**
	 * Liczba klatek symulacji na jedną klatkę animacji (zapisywanej do pliku
	 * AVI).
	 */
	private int simFramesPerAviFrame = 1;

	private AVIWriter out = null;
	private Graphics2D g = null;
	private Graphics2D unclippedG = null;
	private BufferedImage img = null;
	private BufferedImage unclippedImg = null;
	private String aviFilename = "out.avi";
	private boolean isRecording = false;

	public AviRecorder(MallFrame frame) {
		this.frame = frame;
	}

	public int getSimFramesPerAviFrame() {
		return simFramesPerAviFrame;
	}

	public void setSimFramesPerAviFrame(int simFramesPerAviFrame) {
		this.simFramesPerAviFrame = simFramesPerAviFrame;
	}

	public void prepareAvi() throws IOException, AWTException {
		Format format = new Format(EncodingKey, ENCODING_AVI_MJPG, DepthKey,
				24, QualityKey, 1f);

		Rectangle dim = frame.getBoard().getVisibleRect();

		// Make the format more specific
		format = format
				.prepend(MediaTypeKey, MediaType.VIDEO, FrameRateKey,
						new Rational(30, 1), WidthKey, dim.width, HeightKey,
						dim.height);

		img = new BufferedImage(dim.width, dim.height,
				BufferedImage.TYPE_INT_RGB);
		g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		Dimension unclippedDim = frame.getBoard().getSize();
		unclippedImg = new BufferedImage(unclippedDim.width,
				unclippedDim.height, BufferedImage.TYPE_INT_RGB);
		unclippedG = unclippedImg.createGraphics();
		unclippedG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		out = new AVIWriter(new File(aviFilename));
		out.addTrack(format);
		out.setPalette(0, img.getColorModel());

		isRecording = true;
	}

	public void finalizeAvi() throws IOException, AWTException {
		if (out != null) {
			out.close();
		}

		isRecording = false;
	}

	public void recordFrame() {
		if (isRecording) {
			frame.getBoard().paint(unclippedG);

			Rectangle r = frame.getBoard().getVisibleRect();

			int x, y;
			x = (r.x + img.getWidth() <= unclippedImg.getWidth()) ? r.x
					: unclippedImg.getWidth() - img.getWidth();
			y = (r.y + img.getHeight() <= unclippedImg.getHeight()) ? r.y
					: unclippedImg.getHeight() - img.getHeight();

			img = unclippedImg.getSubimage(x, y, img.getWidth(),
					img.getHeight());
			try {
				out.write(0, img, 1);
			} catch (IOException e) {
				System.err.println("AVI write");
				e.printStackTrace();

				try {
					finalizeAvi();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
