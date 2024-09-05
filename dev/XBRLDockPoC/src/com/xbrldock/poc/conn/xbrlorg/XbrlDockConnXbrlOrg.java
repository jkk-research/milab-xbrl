package com.xbrldock.poc.conn.xbrlorg;

import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsJson;

public class XbrlDockConnXbrlOrg implements XbrlDockConnXbrlOrgConsts {

	public void test() throws Exception {
		Map<String, Map<String, Object>> reports = XbrlDockUtilsJson.readJson("temp/allReports.json");

		Map<String, String> repErr = new TreeMap<>();
		Map<String, String> zipErr = new TreeMap<>();

		XbrlDock.log(XbrlEventLevel.Info, "Report count", reports.size());

		int uaCount = 0;
		int esefCount = 0;

		for (Map.Entry<String, Map<String, Object>> er : reports.entrySet()) {
			String repId = er.getKey();

			if (repId.contains("UAIFRS")) {
				++uaCount;
				continue;
			}

			++esefCount;

			Map<String, Object> ri = er.getValue();

			String rep = (String) ri.get("report_url");
			String zip = (String) ri.get("package_url");

			if (XbrlDockUtils.isEmpty(rep)) {
				repErr.put(repId, "MISSING");
			} else if (!rep.endsWith("html")) {
				repErr.put(repId, rep);
			}

			if (XbrlDockUtils.isEmpty(zip)) {
				zipErr.put(repId, "MISSING");
			} else if (!zip.endsWith(".zip")) {
				zipErr.put(repId, zip);
			}
		}

		XbrlDock.log(XbrlEventLevel.Info, "ESEF", esefCount, "UAIFRS", uaCount, "repErrCount", repErr.size(), "zipErrCount",
				zipErr.size());

		XbrlDock.log(XbrlEventLevel.Info, "Report not xhtml", repErr);
		XbrlDock.log(XbrlEventLevel.Info, "Package question", zipErr);

	}
}
