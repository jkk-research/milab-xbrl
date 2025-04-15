package org.xbrldock.vsme.poc;

import java.util.Map;
import java.util.TreeMap;

import org.jsoup.nodes.Document;

import com.xbrldock.XbrlDock;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsConsts;

public interface VsmePocConsts extends XbrlDockPocConsts, XbrlDockUtilsConsts {
	String VSME_standard = "standard";
	String VSME_meta = "meta";

	String VSME_panels = "panels";
	String VSME_items = "items";
	String VSME_options = "options";
	String VSME_expressions = "expressions";

	String VSME_rowSource = "rowSource";
	String VSME_fill = "fill";
	String VSME_calc = "calc";

	String VSME_protect = "vsme_protect";

	String VSME_msgLevel = "msgLevel";
	String VSME_msgRef = "msgRef";
	String VSME_msgExpr = "msgExpr";

	String VSME_AGENT_reportFrame = "reportFrame";

	public interface EditContext {
		Map<String, Object> getMeta();

		Document getStandard();

		Map<String, Object> getReport();

		Object setReportValue(Object val, String attId, int rowIndex, String colId);

		default Object setReportValue(Object val, String attId) {
			return setReportValue(val, attId, -1, null);
		};

		void activateEditor(Object editor, Map<String, Object> p);
	}

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
