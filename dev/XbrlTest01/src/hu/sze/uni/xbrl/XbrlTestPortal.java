package hu.sze.uni.xbrl;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.uni.http.DustHttpServerJetty;
import hu.sze.uni.http.DustHttpServlet;

@SuppressWarnings("rawtypes")
public class XbrlTestPortal implements XbrlConsts {

	private File dataRoot;
	private XbrlFilingManager filings;

	private static final String[] URL_NAMES = { 
			"package_url",
//			"report_url", 
			"json_url", };

	private DustHttpServlet srvReportList = new DustHttpServlet() {
		private static final long serialVersionUID = 1L;

		@Override
		protected void processRequest(Map data) throws Exception {
			Dust.dumpObs("get report list", data);

			PrintWriter out = getWriter(data);
			out.println("<table>\n" 
					+ "	<thead>\n" 
					+ "		<tr>\n" 
					+ "			<th>Country</th>\n" 
					+ "			<th>Period Ending</th>\n" 
					+ "			<th>Entity</th>\n" 
					+ "			<th>Date Added</th>\n"
					+ "			<th>Data</th>\n" 
					+ "			<th>Zip</th>\n"
//					+ "			<th>Rep</th>\n"
					+ "			<th>Json</th>\n" 
					+ "			<th>E</th>\n" 
					+ "			<th>W</th>\n" 
					+ "			<th>I</th>\n" 
					+ "		</tr>\n" 
					+ "	</thead>\n" 
					+ "	<tbody>");

			Pattern ptDateOnly = Pattern.compile("(\\d+-\\d+-\\d+).*");

			for (Map rep : filings.reportData.values()) {
				Object repId = rep.get(XbrlFilingManager.REPORT_ID);

				String da = (String) rep.get("date_added");

				Matcher m = ptDateOnly.matcher(da);
				if ( m.matches() ) {
					da = m.group(1);
				}

				out.println(
							"		<tr>\n" 
						+ "			<td >" + rep.get("country") + "</td>\n" 
						+ "			<td >" + rep.get("period_end") + "</td>\n" 
						+ "			<td >" + rep.get(XbrlFilingManager.ENTITY_NAME) + "</td>\n"
						+ "			<td >" + da + "</td>\n" 
						+ "			<td ><a href=\"/report/" + repId + "\">Show</a></td>\n");

				for (String urlName : URL_NAMES) {
					String addr = (String) rep.get(urlName);
					out.println("			<td >" + (DustUtils.isEmpty(addr) ? " - " : "<a href=\"/bin?type=" + urlName + "&id=" + repId + "\">Get</a>") + "</td>\n");
				}

				out.println(
							"			<td >" + rep.get("error_count") + "</td>\n" 
						+ "			<td >" + rep.get("warning_count") + "</td>\n" 
						+ "			<td >" + rep.get("inconsistency_count") + "</td>\n" 
						+ "		</tr>");
			}

			out.println(
					"	</tbody>\n" 
				+ "</table>");

			Dust.access(data, MindAccess.Set, CONTENT_HTML, ServletData.ContentType);
		}
	};

	private DustHttpServlet srvReport = new DustHttpServlet() {
		private static final long serialVersionUID = 1L;

		@Override
		protected void processRequest(Map data) throws Exception {
			Dust.dumpObs("get report data", data);
			
			String id = Dust.access(data, MindAccess.Peek, null, ServletData.Command);

			Map rep = filings.reportData.get(id);
			String lDir = Dust.access(rep, MindAccess.Peek, null, XbrlFilingManager.LOCAL_DIR);
			String url = Dust.access(rep, MindAccess.Peek, null, "package_url");

			File f = filings.getReport(url, lDir, XbrlReportType.Data);

			HttpServletResponse resp = Dust.access(data, MindAccess.Peek, null, ServletData.Response);

			resp.setContentType(CONTENT_TEXT);
//			resp.setContentType(CONTENT_JSON);

			OutputStream out = getOutStream(data);
			Files.copy(f.toPath(), out);
			out.flush();			
		}
	};

	private DustHttpServlet srvBinary = new DustHttpServlet() {
		private static final long serialVersionUID = 1L;

		@Override
		protected void processRequest(Map data) throws Exception {
			Dust.dumpObs("get binary content", data);

			String id = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "id");
			String type = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "type");

			Map rep = filings.reportData.get(id);
			String lDir = Dust.access(rep, MindAccess.Peek, null, XbrlFilingManager.LOCAL_DIR);
			String url = Dust.access(rep, MindAccess.Peek, null, type);

			String cType = CONTENT_TEXT;
			XbrlReportType repType = null;

			HttpServletResponse resp = Dust.access(data, MindAccess.Peek, null, ServletData.Response);

			switch ( type ) {
			case "package_url":
				String fn = rep.get("fxo_id") + ".zip";
				resp.setHeader("Content-Disposition", "attachment; filename=" + fn);
				cType = CONTENT_ZIP  + "; filename=" + fn + ".zip";
				repType = XbrlReportType.Zip;
				break;
			case "json_url":
				cType = CONTENT_JSON;
				repType = XbrlReportType.Json;
				break;
			}

			if ( null != repType ) {
				resp.setContentType(cType);

				File f = filings.getReport(url, lDir, repType);
				OutputStream out = getOutStream(data);
				Files.copy(f.toPath(), out);
				out.flush();
			} else {
				resp.setContentType(cType);
				getWriter(data).println("Here comes the binary content " + data);
			}
		}
	};

	public XbrlTestPortal() {
		dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
	}

	void init() throws Exception {

		filings = new XbrlFilingManager(dataRoot, true);
		filings.downloadOnly = false;

		initJetty();

	}

	void initJetty() throws Exception {

		DustHttpServerJetty srv = new DustHttpServerJetty() {
			@Override
			protected void initHandlers() {
				super.initHandlers();

				addServlet("/list/*", srvReportList);
				addServlet("/report/*", srvReport);
				addServlet("/bin/*", srvBinary);
			}
		};
		srv.activeInit();
	}

	public static void main(String[] args) throws Exception {
		Dust.main(args);

		XbrlTestPortal portal = new XbrlTestPortal();

		portal.init();
	}

}
