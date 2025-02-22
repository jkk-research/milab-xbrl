package org.xbrldock.vsme.poc;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.nodes.Document;

import com.xbrldock.XbrlDock;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsHtml;
import com.xbrldock.utils.XbrlDockUtilsJson;

public class VsmeMain extends XbrlDock implements VsmePocConsts {

	Document standard;
	Map<String, Object> meta;
	Map<String, Object> params;

	@Override
	public Object process(String cmd, Map<String, Object> params) throws Exception {
		switch (cmd) {
		case XDC_CMD_GEN_Init:
			this.params = params;

			String fName = XbrlDockUtils.simpleGet(params, VSME_meta);
			meta = XbrlDockUtilsJson.readJson(new File(fName));

			fName = XbrlDockUtils.simpleGet(params, VSME_standard);
			standard = XbrlDockUtilsHtml.readHtml(fName);
//			standard = XbrlDockUtilsXml.parseDoc(new File(fName));
//
//			VsmeTest.testStandard(standard);

			break;
		case XDC_CMD_GEN_Begin:
			Map<String, Object> fp = new TreeMap<String, Object>();
			
			fp.put(VSME_meta, meta);
			fp.put(VSME_standard, standard);
			XbrlDock.callAgent(VSME_AGENT_reportFrame, XDC_CMD_GEN_Begin, fp);
			break;
		}
		return null;
	}

}
