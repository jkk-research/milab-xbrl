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

	Map<String, Map<String, Object>> unitDef = new TreeMap<>();
	Map<String, Map<String, Object>> ctxDef = new TreeMap<>();

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
		XbrlDockUtils.safeGet(reportData, XDC_REPORT_TOKEN_namespaces, MAP_CREATOR).put(ref, id);
	}

	@Override
	public void addTaxonomy(String tx) {
		XbrlDockUtils.safeGet(reportData, XDC_REPORT_TOKEN_schemas, ARRAY_CREATOR).add(tx);
	}

	@Override
	public String processSegment(String segment, Map<String, Object> data) {
		String ret = "";
		String sVal;

		switch (segment) {
		case XDC_REP_SEG_Unit:
			ret = (String) data.get(XDC_FACT_TOKEN_unit);
			if (XbrlDockUtils.isEmpty(ret)) {
				ret = "unit-" + unitDef.size();
			}
			unitDef.put(ret, new TreeMap<String, Object>(data));
			break;
		case XDC_REP_SEG_Context:
			ret = (String) data.get(XDC_FACT_TOKEN_context);
			if (XbrlDockUtils.isEmpty(ret)) {
				ret = "ctx-" + ctxDef.size();
			}
			ctxDef.put(ret, new TreeMap<String, Object>(data));
			
			
			sVal = (String) data.get(XDC_FACT_TOKEN_instant);
			if (XbrlDockUtils.isEmpty(sVal)) {
				String cs = (String) data.get(XDC_FACT_TOKEN_startDate);
				
				if ( (null == repStart) || (0 < repStart.compareTo(cs)) ) {
					repStart = cs;
				}
				
				String ce = (String) data.get(XDC_FACT_TOKEN_endDate);
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
		case XDC_REP_SEG_Fact:
			ret = (String) data.get(XDC_FACT_TOKEN_id);
			if (XbrlDockUtils.isEmpty(ret)) {
				ret = "fact-" + (factCount++);
			}
			break;
		}

		return ret;
	}

	@Override
	public void endReport() {
		reportData.put(XDC_REPORT_TOKEN_startDate, repStart);
		reportData.put(XDC_REPORT_TOKEN_endDate, repEnd);
	}
}
