package com.xbrldock.dev;

import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts;

@SuppressWarnings({ "unchecked" })
public class XbrlDockDevReportDump implements XbrlDockDevConsts, XbrlDockConsts.GenAgent {

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
	public Object process(String cmd, Map<String, Object> params) throws Exception {
		switch (cmd) {
		case XDC_CMD_GEN_Init:
			break;
		case XDC_CMD_GEN_Begin:
			beginReport((String) params.get(XDC_EXT_TOKEN_id));
			break;
		case XDC_CMD_REP_ADD_NAMESPACE:
			addNamespace((String) params.get(XDC_EXT_TOKEN_id), (String) params.get(XDC_EXT_TOKEN_value));
			break;
		case XDC_CMD_REP_ADD_SCHEMA:
			addTaxonomy((String) params.get(XDC_EXT_TOKEN_id), (String) params.get(XDC_EXT_TOKEN_value));
			break;
		case XDC_REP_SEG_Unit:
		case XDC_REP_SEG_Context:
		case XDC_REP_SEG_Fact:
			return processSegment(cmd, (Map<String, Object>) params.get(XDC_GEN_TOKEN_source));
		case XDC_CMD_GEN_End:
			endReport();
			break;
		}
		return null;
	}

	public void beginReport(String repId) {
		XbrlDock.log(EventLevel.Info, "\n\n --- Begin report", repId, "---");
		reset();
		this.repId = repId;
	}

	public void addNamespace(String ref, String id) {
//		XbrlDock.log(EventLevel.Info, "Added namespace", ref, id);
	}

	public void addTaxonomy(String tx, String type) {
//		XbrlDock.log(EventLevel.Info, "Added taxonomy", tx);
	}

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
