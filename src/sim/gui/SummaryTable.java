package sim.gui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JTable;
import javax.swing.table.TableModel;

@SuppressWarnings("serial")
public class SummaryTable extends JTable {

	public static enum Param {
		PERC_OF_FIELDS_AS_LANES, LANES_COHERENCE, LOST, AVG_DISTANCE
	}

	int samples = 0;

	private static Map<Param, Object> paramValues = new HashMap<Param, Object>();
	private static Map<Param, Double> avgParamValues = new HashMap<Param, Double>();

	private static Object[][] rawData = { { "% of fields as lanes", "" },
			{ "lanes' coherence", "" }, { "lost", "" }, { "avg distance", "" },
			{ "% of fields as lanes (avg)", "" },
			{ "lanes' coherence (avg)", "" }, { "lost (avg)", "" }, { "avg distance (avg)", "" } };

	private static String[] columnNames = { "Parameter", "Value" };

	private final static Logger LOGGER = Logger
			.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	public SummaryTable() {
		super(rawData, columnNames);

		setParamValue(Param.PERC_OF_FIELDS_AS_LANES, Double.valueOf(0.0));
		setParamValue(Param.LANES_COHERENCE, Double.valueOf(0.0));
		setParamValue(Param.LOST, Double.valueOf(0.0));
		setParamValue(Param.AVG_DISTANCE, Double.valueOf(0.0));
	}

	public void setParamValue(Param param, Object value) {
		paramValues.put(param, value);

		Double newVal = Double.valueOf(value.toString());

		if (avgParamValues.containsKey(param)) {
			Double avgVal = avgParamValues.get(param);
			newVal = round((avgVal * (double) samples + newVal)
					/ (double) (samples + 1));
			avgParamValues.put(param, newVal);
		} else {
			avgParamValues.put(param, newVal);
		}

		TableModel m = getModel();
		if (value instanceof Double)
			m.setValueAt(round(value), param.ordinal(), 1);
		else 
			m.setValueAt(value, param.ordinal(), 1);
		
		m.setValueAt(newVal, param.ordinal() + Param.values().length, 1);
	}

	private Double round(Object value) {
		Double dv = 0.0;
		
		try {
			dv = Double.valueOf(value.toString());
		} catch (NumberFormatException e) {
			LOGGER.severe("Number format exception: " + e.getMessage());
		}
		
		BigDecimal bd = new BigDecimal(dv)
				.setScale(2, RoundingMode.HALF_EVEN);
		return bd.doubleValue();
	}

	public void clear() {
		avgParamValues.clear();
		samples = 0;
	}

	public void nextSample() {
		samples++;
	}
}
