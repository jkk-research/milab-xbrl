package com.xbrldock.poc.report;

import java.util.Map;

import com.xbrldock.utils.XbrlDockUtils;

//@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlDockReportUtils implements XbrlDockReportConsts {
	
	
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
		public Object process(String cmd, Object... params) throws Exception {
			Map item = (Map) params[0];
			switch (cmd) {
			case XDC_CMD_GEN_Init:
				match = false;
				ret = true;
				break;
			case XDC_CMD_GEN_Process:
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
