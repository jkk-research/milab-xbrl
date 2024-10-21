package com.xbrldock.poc.report;

import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.utils.XbrlDockUtils;

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
	
	@SuppressWarnings("rawtypes")
	public static class SimpleMatchTester implements ExprResultProcessor {		
		private Object targetValue;
		
		private boolean byContext;
		private boolean ret;
		
		private boolean match;
		
		public SimpleMatchTester() {
			this(true);
		}
		
		public SimpleMatchTester(Object targetValue) {
			setTargetValue(targetValue);
		}
		
		public void setTargetValue(Object targetValue) {
			this.targetValue = targetValue;
		}
		
		public void setByContext(boolean byContext) {
			this.byContext = byContext;
		}

		@Override
		public boolean process(ProcessorAction action, Map item) throws Exception {
			switch (action) {
			case Init:
				match = false;
				ret = true;
				break;
			case Process:
				match = XbrlDockUtils.isEqual(targetValue, item.get(XDC_EXPR_result));
				if ( match ) {
					ret = false;					
				}
				break;

			default:
				break;
			}
			
			return ret;
		}

		@Override
		public boolean isByContext() {
			return byContext;
		}
		
		public boolean isMatch() {
			return match;
		}
		
	}
}
