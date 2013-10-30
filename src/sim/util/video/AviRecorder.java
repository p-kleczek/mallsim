package sim.util.video;

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

import sim.gui.GUIBoard;
import sim.gui.MallFrame;

/**
 * Zapisuje sekwencję zrzutów ekranu obszaru roboczego do pliku wideo (w
 * formacie AVI).
 * 
 * @author Pawel
 * 
 */
public class AviRecorder extends VideoRecorderImpl {

	private MallFrame frame;

	private AVIWriter aviWriter = null;
	private Graphics2D clippedGraphics = null;
	private Graphics2D unclippedGraphics = null;
	private BufferedImage clippedImage = null;
	private BufferedImage unclippedImage = null;
	private boolean isRecording = false;

	public AviRecorder() {
	}

	public void recordFrame() {
		if (isRecording) {
			GUIBoard board = frame.getBoard();
			board.paint(unclippedGraphics);

			Rectangle visibleRect = board.getVisibleRect();
			int x = (visibleRect.x + clippedImage.getWidth() <= unclippedImage.getWidth()) ? visibleRect.x
					: unclippedImage.getWidth() - clippedImage.getWidth();
			int y = (visibleRect.y + clippedImage.getHeight() <= unclippedImage.getHeight()) ? visibleRect.y
					: unclippedImage.getHeight() - clippedImage.getHeight();

			clippedImage = unclippedImage.getSubimage(x, y,
					clippedImage.getWidth(), clippedImage.getHeight());
			try {
				aviWriter.write(0, clippedImage, 1);
			} catch (IOException e) {
				System.err.println("AVI write");
				e.printStackTrace();

				try {
					finish();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	@Override
	public void setSource(MallFrame frame) {
		this.frame = frame;

	}

	@Override
	public void prepare() throws IOException, AWTException {
		Format format = new Format(EncodingKey, ENCODING_AVI_MJPG, DepthKey,
				24, QualityKey, 1f);

		Rectangle visibleRect = frame.getBoard().getVisibleRect();

		// Make the format more specific
		format = format.prepend(MediaTypeKey, MediaType.VIDEO, FrameRateKey,
				new Rational(30, 1), WidthKey, visibleRect.width, HeightKey,
				visibleRect.height);

		clippedImage = new BufferedImage(visibleRect.width, visibleRect.height,
				BufferedImage.TYPE_INT_RGB);
		clippedGraphics = clippedImage.createGraphics();
		clippedGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		Dimension entireRect = frame.getBoard().getSize();
		unclippedImage = new BufferedImage(entireRect.width, entireRect.height,
				BufferedImage.TYPE_INT_RGB);
		unclippedGraphics = unclippedImage.createGraphics();
		unclippedGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		aviWriter = new AVIWriter(new File(getOutputFilename()));
		aviWriter.addTrack(format);
		aviWriter.setPalette(0, clippedImage.getColorModel());

		isRecording = true;

	}

	@Override
	public void finish() throws IOException, AWTException {
		if (aviWriter != null) {
			aviWriter.close();
		}

		isRecording = false;
	}
}
