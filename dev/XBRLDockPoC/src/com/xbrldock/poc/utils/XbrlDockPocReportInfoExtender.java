package com.xbrldock.poc.utils;

import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlDockPocReportInfoExtender implements XbrlDockPocConsts, XbrlDockConsts.ReportDataHandler {

	Map reportData;

	String repStart = null;
	String repEnd = null;
	int factCount = 0;

	String repId;

	Map<String, Map<XbrlFactKeys, Object>> unitDef = new TreeMap<>();
	Map<String, Map<XbrlFactKeys, Object>> ctxDef = new TreeMap<>();

	public void setReportData(Map reportData) {
		this.reportData = reportData;
	}

	@Override
	public void beginReport(String repId) {
		unitDef.clear();
		ctxDef.clear();
		factCount = 0;

		repStart = repEnd = null;
	}

	@Override
	public void addNamespace(String ref, String id) {
		XbrlDockUtils.safeGet(reportData, XbrlFilingKeys.namespaces, MAP_CREATOR).put(ref, id);
	}

	@Override
	public void addTaxonomy(String tx) {
		XbrlDockUtils.safeGet(reportData, XbrlFilingKeys.schemas, ARRAY_CREATOR).add(tx);
	}

	@Override
	public String processSegment(XbrlReportSegment segment, Map<XbrlFactKeys, Object> data) {
		String ret = "";
		String sVal;

		switch (segment) {
		case Unit:
			ret = (String) data.get(XbrlFactKeys.unit);
			if (XbrlDockUtils.isEmpty(ret)) {
				ret = "unit-" + unitDef.size();
			}
			unitDef.put(ret, new TreeMap<XbrlFactKeys, Object>(data));
			break;
		case Context:
			ret = (String) data.get(XbrlFactKeys.context);
			if (XbrlDockUtils.isEmpty(ret)) {
				ret = "ctx-" + ctxDef.size();
			}
			ctxDef.put(ret, new TreeMap<XbrlFactKeys, Object>(data));
			
			
			sVal = (String) data.get(XbrlFactKeys.instant);
			if (XbrlDockUtils.isEmpty(sVal)) {
				String cs = (String) data.get(XbrlFactKeys.startDate);
				
				if ( (null == repStart) || (0 < repStart.compareTo(cs)) ) {
					repStart = cs;
				}
				
				String ce = (String) data.get(XbrlFactKeys.endDate);
				if ( (null == repEnd) || (0 > repEnd.compareTo(ce)) ) {
					repEnd = ce;
				}
			} else {
				if ( (null == repStart) || (0 < repStart.compareTo(sVal)) ) {
					repStart = sVal;
				}
				if ( (null == repEnd) || (0 > repEnd.compareTo(sVal)) ) {
					repEnd = sVal;
				}
			}
			break;
		case Fact:
			ret = (String) data.get(XbrlFactKeys.id);
			if (XbrlDockUtils.isEmpty(ret)) {
				ret = "fact-" + (factCount++);
			}
			break;
		}

		return ret;
	}

	@Override
	public void endReport() {
		XbrlDockUtils.simpleSet(reportData, repStart, XbrlFilingKeys.startDate);
		XbrlDockUtils.simpleSet(reportData, repEnd, XbrlFilingKeys.endDate);
	}
}
