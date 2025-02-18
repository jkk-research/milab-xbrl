package org.xbrldock.vsme.poc;

import java.io.File;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.xbrldock.XbrlDock;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsJson;
import com.xbrldock.utils.XbrlDockUtilsXml;

public class VsmeMain extends XbrlDock implements VsmePocConsts {
	
	Document standard;
	Map<String, Object> meta;

	@Override
	public Object process(String cmd, Map<String, Object> params) throws Exception {
		switch (cmd) {
		case XDC_CMD_GEN_Init:
			

			String fName = XbrlDockUtils.simpleGet(params, VSME_meta);
			meta = XbrlDockUtilsJson.readJson(new File(fName));

			fName = XbrlDockUtils.simpleGet(params, VSME_standard);
			standard = XbrlDockUtilsXml.parseDoc(new File(fName));
			

			NodeList nl = standard.getElementsByTagName("*");
			int nc = nl.getLength();

			for (int idx = 0; idx < nc; ++idx) {
				Element e = (Element) nl.item(idx);
				String tagName = e.getTagName();
				
				String id = e.getAttribute(XDC_EXT_TOKEN_id);
				
				if ( "vsme_UndertakingCategory".equals(id)) {
					XbrlDock.log(null, e.getTextContent());
				}

				switch (tagName) {
				case "import":
					break;
				}
			}
		
			break;
		case XDC_CMD_GEN_Begin:
			break;
		}
		return null;
	}

}
