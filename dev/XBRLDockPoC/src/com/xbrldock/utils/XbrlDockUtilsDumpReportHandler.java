package com.xbrldock.utils;

import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockConsts.ReportSegment;
import com.xbrldock.XbrlDockConsts.XbrlEventLevel;
import com.xbrldock.XbrlDockConsts.XbrlToken;

public class XbrlDockUtilsDumpReportHandler implements XbrlDockConsts.ReportDataHandler {

	public boolean logAll = true;

	int factCount = 0;

	String repId;

	Map<String, Map<XbrlToken, Object>> unitDef = new TreeMap<>();
	Map<String, Map<XbrlToken, Object>> ctxDef = new TreeMap<>();

	public void reset() {
		unitDef.clear();
		ctxDef.clear();
		factCount = 0;
	}

	@Override
	public void beginReport(String repId) {
		XbrlDock.log(XbrlEventLevel.Info, "\n\n --- Begin report", repId, "---");
		reset();
		this.repId = repId;
	}

	@Override
	public void addNamespace(String ref, String id) {
		XbrlDock.log(XbrlEventLevel.Info, "Added namespace", ref, id);
	}

	@Override
	public void addTaxonomy(String tx) {
		XbrlDock.log(XbrlEventLevel.Info, "Added taxonomy", tx);
	}

	@Override
	public String processSegment(ReportSegment segment, Map<XbrlToken, Object> data) {
		String ret = "";

		switch (segment) {
		case Unit:
			ret = "unit-" + unitDef.size();
			unitDef.put(ret, new TreeMap<XbrlToken, Object>(data));
			break;
		case Context:
			ret = "ctx-" + ctxDef.size();
			ctxDef.put(ret, new TreeMap<XbrlToken, Object>(data));
			break;
		case Fact:
			ret = (String) data.get(XbrlToken.id);
			break;
		}

		if (logAll) {
			XbrlDock.log(XbrlEventLevel.Info, "Processing segment", segment, ret, data);
		}

		return ret;
	}

	@Override
	public void endReport() {
		if (logAll) {
			XbrlDock.log(XbrlEventLevel.Info, " --- End report", repId, "---\n");
		}
	}
}
