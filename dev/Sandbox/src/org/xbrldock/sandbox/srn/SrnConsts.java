package org.xbrldock.sandbox.srn;

import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsConsts;

public interface SrnConsts extends XbrlDockPocConsts, XbrlDockUtilsConsts {
	String SBX_SRN_ERROR = "error";
	
	
	public class ExprCtx {
		Map<String, Object> data = new TreeMap<String, Object>();
		String exprId;

		public <RetType> RetType get(Object root, Object... path) {
			return XbrlDockUtils.simpleGet((null == root) ? data : root, path);
		}

		public void setMessage(String msgCode, String msgText) {
			XbrlDock.log(EventLevel.Error, exprId, msgCode, msgText);
		}
	};

}
