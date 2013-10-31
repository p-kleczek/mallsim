package sim.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class WriterUtils {

	public static DecimalFormat decimalFormat;

	static {
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(
				Locale.getDefault());
		otherSymbols.setDecimalSeparator(',');
		decimalFormat = new DecimalFormat("#.00", otherSymbols);
	}
}
