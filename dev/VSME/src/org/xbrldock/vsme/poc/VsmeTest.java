package org.xbrldock.vsme.poc;

import java.util.Collection;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.xbrldock.XbrlDock;

public class VsmeTest implements VsmePocConsts {

	public static void testStandard(Document standard) {
		NodeList nl = standard.getElementsByTagName("*");
		int nc = nl.getLength();

		for (int idx = 0; idx < nc; ++idx) {
			Element e = (Element) nl.item(idx);
			String tagName = e.getTagName();

			String id = e.getAttribute(XDC_EXT_TOKEN_id);

			if ("vsme_UndertakingCategory".equals(id)) {
				XbrlDock.log(null, e.getTextContent());
			}

			switch (tagName) {
			case "import":
				break;
			}
		}
	}
	
	public static Object categoryExpr(ExprCtx ectx) {
		Collection<Map<String, Object>> options = ectx.get(null, "target", "options");
		int above;

		for (Map<String, Object> od : options) {
			above = 0;

			Map<String, Number> limits = ectx.get(od, "limits");
			for (Map.Entry<String, Number> le : limits.entrySet()) {
				Number rv = ectx.get(null, "report", le.getKey());
				if (null == rv) {
					ectx.setMessage("Missing field", le.getKey());
					return null;
				}
				if (rv.doubleValue() > le.getValue().doubleValue()) {
					++above;
				}
			}

			if (above < 2) {
				return ectx.get(od, "id");
			}
		}
		ectx.setMessage("Invalid category", "Above Medium level limits");
		return null;
	};

}
