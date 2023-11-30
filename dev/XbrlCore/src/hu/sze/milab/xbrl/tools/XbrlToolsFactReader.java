package hu.sze.milab.xbrl.tools;

import java.util.Set;
import java.util.TreeSet;

import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsData;

public class XbrlToolsFactReader {
	private DustUtilsData.TableReader tr;

	private int diStart;
	private int diEnd;

	private Set<String> axes = new TreeSet<>();
	private String contextId;
	private String axisId;

	public void setTableReader(DustUtilsData.TableReader tableReader) {
		this.tr = tableReader;

		diStart = tableReader.getColIdx("Instant");
		diEnd = tableReader.getColIdx("OrigValue");
	}

	public void readFact(String[] rf) {
		String time = tr.get(rf, "Instant");
		if ( DustUtils.isEmpty(time) ) {
			time = tr.format(rf, " / ", "StartDate", "EndDate");
		}

		axes.clear();
		for (int i = diStart + 1; i < diEnd; i += 2) {
			String aVal = rf[i];
			if ( !DustUtils.isEmpty(aVal) ) {
				axes.add(aVal);
			}
		}

		axisId = axes.isEmpty() ? "" : (" " + axes.toString());
		contextId = " (" + time + ")" + axisId;
	}
	
	public String getContextId() {
		return contextId;
	}
	
	public String getAxisId() {
		return axisId;
	}
	
	public boolean isPrimary() {
		return DustUtils.isEmpty(axisId);
	}
}