package sim.gui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import sim.model.Agent;

@SuppressWarnings("serial")
public class SummaryTable extends JTable {

	public static enum Param {
		PERC_OF_FIELDS_AS_LANES, LANES_COHERENCE, LOST
	}

	int samples = 0;

	private static Map<Param, Object> paramValues = new HashMap<Param, Object>();
	private static Map<Param, Double> avgParamValues = new HashMap<Param, Double>();

	private static Object[][] rawData = { { "% of fields as lanes", "" },
			{ "lanes' coherence", "" }, { "lost", "" },
			{ "% of fields as lanes (avg)", "" },
			{ "lanes' coherence (avg)", "" }, { "lost (avg)", "" } };

	private static String[] columnNames = { "Parameter", "Value" };

	public SummaryTable() {
		super(rawData, columnNames);

		setParamValue(Param.PERC_OF_FIELDS_AS_LANES, Double.valueOf(0.0));
		setParamValue(Param.LANES_COHERENCE, Double.valueOf(0.0));
		setParamValue(Param.LOST, Double.valueOf(0.0));
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
		m.setValueAt(value, param.ordinal(), 1);
		m.setValueAt(newVal, param.ordinal() + Param.values().length, 1);
	}

	Double round(Object value) {
		BigDecimal bd = new BigDecimal(Double.valueOf(value.toString()))
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
