package com.xbrldock.utils.stream;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlDockStreamUtils implements XbrlDockStreamConsts {

	public static void json2Csv(File from, File to, String sepChar, String... colNames) throws Exception {
		ArrayList<Map<String, Object>> arr = XbrlDockStreamJson.readJson(from);
		
		if ( arr.isEmpty() ) {
			return;
		}

		ArrayList<String> cols = new ArrayList<>();

		for (String c : colNames) {
			XbrlDockUtils.optAdd(cols, c);
		}

		for (Map<String, Object> item : arr) {
			XbrlDockUtils.optAdd(cols, item.keySet());
		}

		if ( cols.isEmpty() ) {
			return;
		}

		Map p = new TreeMap();
		XbrlDockUtilsFile.ensureDir(to.getParent());

		try (XbrlDockStreamCsvWriterAgent w = new XbrlDockStreamCsvWriterAgent()) {
			p.put(XDC_GEN_TOKEN_members, cols.toArray());
			p.put(XDC_GEN_TOKEN_target, to.getCanonicalPath());

			w.process(XDC_CMD_GEN_Init, p);
			w.process(XDC_CMD_GEN_Begin, p);

			for (Map<String, Object> item : arr) {
				p.put(XDC_EXT_TOKEN_value, item);
				
//				if ( ((String) item.getOrDefault("error", "")).contains("Unknown encryption type R = 6") ) {
//					item.remove("error");
//				}
				w.process(XDC_CMD_GEN_Process, p);
			}
		}
	}

}
