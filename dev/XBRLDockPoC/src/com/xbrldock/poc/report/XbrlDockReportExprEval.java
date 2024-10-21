package com.xbrldock.poc.report;

import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsMvel;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockReportExprEval implements XbrlDockReportConsts, XbrlDockPocConsts.ReportDataHandler {

	String exprStr;
	Object exprComp;
	
	String repId;
	Map segData = new TreeMap<>();
	
	public void setExpression(String expr) {
		exprComp = XbrlDockUtilsMvel.compile(expr);
	}

	@Override
	public void beginReport(String repId) {
		this.repId = repId;
	}

	@Override
	public void addNamespace(String ref, String id) {
	}

	@Override
	public void addTaxonomy(String tx) {
	}

	@Override
	public String processSegment(String segment, Map<String, Object> data) {
		Map segContent = XbrlDockUtils.safeGet(segData, segment, SORTEDMAP_CREATOR);

		String segIdKey = XbrlDockReportUtils.getSegmentIdKey(segment);
		String segId = (String) data.get(segIdKey);
		if ( XbrlDockUtils.isEmpty(segId) ) {
			segId = segment + "-" + (segContent.size() + 1);
		}
		
		TreeMap<String, Object> cloneData = new TreeMap<String, Object>(data);
		
		switch ( segment ) {
		case XDC_REP_SEG_Unit:
		case XDC_REP_SEG_Context:
			segContent.put(segId, cloneData);
			break;
		case XDC_REP_SEG_Fact:
			String ctxId = (String) data.get(XDC_FACT_TOKEN_context);
			XbrlDockUtils.safeGet(segContent, ctxId, SET_CREATOR).add(cloneData);
			break;
			default:
				break;
		}
		
		return segId;
	}

	@Override
	public void endReport() {
		
	}

}
