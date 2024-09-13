package com.xbrldock.dev;

import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts;

public class XbrlDockDevReportDump implements XbrlDockDevConsts, XbrlDockConsts.ReportDataHandler {

	public boolean logAll = true;

	int factCount = 0;

	String repId;

	Map<String, Map<XbrlFactKeys, Object>> unitDef = new TreeMap<>();
	Map<String, Map<XbrlFactKeys, Object>> ctxDef = new TreeMap<>();

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
		XbrlDock.log(EventLevel.Info, "Added namespace", ref, id);
	}

	@Override
	public void addTaxonomy(String tx) {
		XbrlDock.log(EventLevel.Info, "Added taxonomy", tx);
	}

	@Override
	public String processSegment(XbrlReportSegment segment, Map<XbrlFactKeys, Object> data) {
		String ret = "";

		switch (segment) {
		case Unit:
			ret = "unit-" + unitDef.size();
			unitDef.put(ret, new TreeMap<XbrlFactKeys, Object>(data));
			break;
		case Context:
			ret = "ctx-" + ctxDef.size();
			ctxDef.put(ret, new TreeMap<XbrlFactKeys, Object>(data));
			break;
		case Fact:
			ret = (String) data.get(XbrlFactKeys.id);
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
}
