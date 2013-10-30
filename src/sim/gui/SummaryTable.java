package sim.gui;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import sim.model.Agent;

@SuppressWarnings("serial")
public class SummaryTable extends JTable {

	public static enum Param {
		PERC_OF_FIELDS_AS_LANES
	}

	private static Map<Param, Object> paramValues = new HashMap<Param, Object>();

	private static Object[][] rawData = { { "% of fields as lanes", "" } };

	private static String[] columnNames = { "Parameter", "Value" };

	public SummaryTable() {
		super(rawData, columnNames);

		setParamValue(Param.PERC_OF_FIELDS_AS_LANES, Double.valueOf(0.0));
	}

	public void setParamValue(Param param, Object value) {
		paramValues.put(param, value);

		TableModel m = getModel();
		m.setValueAt(value, param.ordinal(), 1);
	}
}
