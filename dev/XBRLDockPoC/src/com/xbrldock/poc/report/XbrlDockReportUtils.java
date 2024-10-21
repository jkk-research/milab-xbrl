package com.xbrldock.poc.report;

import java.util.Map;
import java.util.TreeMap;

//@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlDockReportUtils implements XbrlDockReportConsts {
	
	private static final Map<String, String> SEGMENT_IDS = new TreeMap<>();
	
	static {
		SEGMENT_IDS.put(XDC_REP_SEG_Unit, XDC_FACT_TOKEN_unit);
		SEGMENT_IDS.put(XDC_REP_SEG_Context, XDC_FACT_TOKEN_context);
		SEGMENT_IDS.put(XDC_REP_SEG_Fact, XDC_EXT_TOKEN_id);
	}
	
	public static String getSegmentIdKey(String seg) {
		return SEGMENT_IDS.get(seg);
	}
}
