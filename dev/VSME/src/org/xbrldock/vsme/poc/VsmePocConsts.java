package org.xbrldock.vsme.poc;

import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsConsts;

public interface VsmePocConsts extends XbrlDockPocConsts, XbrlDockUtilsConsts {
	String VSME_standard = "standard";
	String VSME_meta = "meta";
	
	String VSME_start = "start";
	String VSME_panels = "panels";
	String VSME_items = "items";
	String VSME_attributes = "attributes";
	String VSME_options = "options";
	String VSME_expressions = "expressions";
	
	String VSME_rowSource = "rowSource";
	String VSME_rows = "rows";
	String VSME_fill = "fill";
	String VSME_calc = "calc";
	
	
	String VSME_protect = "vsme_protect";
	
	String VSME_msgLevel = "msgLevel";
	String VSME_msgRef = "msgRef";
	String VSME_msgExpr = "msgExpr";
	
	String VSME_AGENT_reportFrame = "reportFrame";
	
	
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
