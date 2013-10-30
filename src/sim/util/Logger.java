package sim.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logger class for logging.
 */

public class Logger {
	public static enum Level {
		INFO, ERROR
	}

	private static Level minimumLevel = Level.ERROR;

	public static void log(String what, Level severity) {
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");

		if (severity.ordinal() >= minimumLevel.ordinal()) {
			System.out.println(String.format("[%s] %s",
					format.format(new Date()), what));
		}
	}
}