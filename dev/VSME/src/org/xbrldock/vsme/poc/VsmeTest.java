package org.xbrldock.vsme.poc;

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
}
