package com.xbrldock.dev;

import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.poc.XbrlDockPocConsts;

public class XbrlDockDevReportDump implements XbrlDockDevConsts, XbrlDockPocConsts.ReportDataHandler {

	public boolean logAll = true;

	int factCount = 0;

	String repId;

	Map<String, Map<String, Object>> unitDef = new TreeMap<>();
	Map<String, Map<String, Object>> ctxDef = new TreeMap<>();

	public void reset() {
		unitDef.clear();
		ctxDef.clear();
		factCount = 0;
	}

	@Override
	public void beginReport(String repId) {
		XbrlDock.log(EventLevel.Info, "\n\n --- Begin report", repId, "---");
		reset();
		this.repId = repId;
	}

	@Override
	public void addNamespace(String ref, String id) {
//		XbrlDock.log(EventLevel.Info, "Added namespace", ref, id);
	}

	@Override
	public void addTaxonomy(String tx, String type) {
//		XbrlDock.log(EventLevel.Info, "Added taxonomy", tx);
	}

	@Override
	public String processSegment(String segment, Map<String, Object> data) {
		String ret = "";

		switch (segment) {
		case XDC_REP_SEG_Unit:
			ret = "unit-" + unitDef.size();
			unitDef.put(ret, new TreeMap<String, Object>(data));
			break;
		case XDC_REP_SEG_Context:
			ret = "ctx-" + ctxDef.size();
			ctxDef.put(ret, new TreeMap<String, Object>(data));
			break;
		case XDC_REP_SEG_Fact:
			ret = (String) data.get(XDC_EXT_TOKEN_id);
			++factCount;
			break;
		}

		if (logAll) {
			XbrlDock.log(EventLevel.Info, "Processing segment", segment, ret, data);
		}

		return ret;
	}

	@Override
	public void endReport() {
		if (logAll) {
			XbrlDock.log(EventLevel.Info, " --- End report", repId, "---\n");
		}
	}

	public int getCount(String segment) {
		switch (segment) {
		case XDC_REP_SEG_Unit:
			return unitDef.size();
		case XDC_REP_SEG_Context:
			return ctxDef.size();
		case XDC_REP_SEG_Fact:
			return factCount;
		}
		return 0;
	}
}
