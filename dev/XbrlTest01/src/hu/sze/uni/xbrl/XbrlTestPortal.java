package hu.sze.uni.xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;
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

//	private static final String[] URL_NAMES = { 
//			"package_url",
////			"report_url", 
//			"json_url", };

	private DustHttpServlet srvReportList = new DustHttpServlet() {
		private static final long serialVersionUID = 1L;

		@Override
		protected void processRequest(Map data) throws Exception {
			Dust.dumpObs("get report list", data);

		//@formatter:off
			PrintWriter out = getWriter(data);
			
			out.print("<html lang=\"en-US\">\n"
					+ "<head>\n"
					+ "<title>Report list</title>\n"
					+ "\n"
					+ "<style>\n"
					+ "table, th, td {\n"
					+ "  border: 1px solid black;\n"
					+ "}"
					+ "</style>\n"
					+ "</head>\n"
					+ "<body>");

			out.println("<table>\n" 
					+ "	<thead>\n" 
					+ "		<tr>\n" 
					+ "			<th>Country</th>\n" 
					+ "			<th>Period Ending</th>\n" 
					+ "			<th>Entity</th>\n" 
					+ "			<th>Date Added</th>\n"
					+ "			<th>Content</th>\n" 
					+ "			<th>CSV Num</th>\n" 
					+ "			<th>CSV Txt</th>\n" 
					+ "			<th>Zip</th>\n"
					+ "			<th>Json</th>\n" 
					+ "			<th>E</th>\n" 
					+ "			<th>W</th>\n" 
					+ "			<th>I</th>\n" 
					+ "		</tr>\n" 
					+ "	</thead>\n" 
					+ "	<tbody>");
		//@formatter:on

			Pattern ptDateOnly = Pattern.compile("(\\d+-\\d+-\\d+).*");

			for (Map rep : filings.reportData.values()) {
				Object repId = rep.get(XbrlFilingManager.REPORT_ID);

				String da = (String) rep.get("date_added");

				Matcher m = ptDateOnly.matcher(da);
				if ( m.matches() ) {
					da = m.group(1);
				}

			//@formatter:off
				out.println(
							"		<tr>\n" 
						+ "			<td >" + rep.get("country") + "</td>\n" 
						+ "			<td >" + rep.get("period_end") + "</td>\n" 
						+ "			<td >" + rep.get(XbrlFilingManager.ENTITY_NAME) + "</td>\n"
						+ "			<td >" + da + "</td>\n" 
						+ "			<td ><a href=\"/report/" + repId + "\">" + repId + "</a></td>\n"
						+ "			<td ><a href=\"/bin?type=csv&ct=num&id=" + repId + "\">CSV Num</a></td>\n"
						+ "			<td ><a href=\"/bin?type=csv&ct=txt&id=" + repId + "\">CSV Txt</a></td>\n");
			//@formatter:on

				String addr;
				addr = (String) rep.get("package_url");
				out.println("			<td >" + (DustUtils.isEmpty(addr) ? " - " : "<a href=\"/bin?type=package_url&id=" + repId + "\">Zip</a>") + "</td>\n");
				addr = (String) rep.get("json_url");
				out.println("			<td >" + (DustUtils.isEmpty(addr) ? " - " : "<a href=\"/bin?type=json_url&id=" + repId + "\">JSON</a>") + "</td>\n");

				//@formatter:off

				out.println(
							"			<td >" + rep.get("error_count") + "</td>\n" 
						+ "			<td >" + rep.get("warning_count") + "</td>\n" 
						+ "			<td >" + rep.get("inconsistency_count") + "</td>\n" 
						+ "		</tr>");
			//@formatter:on

			}

		//@formatter:off
			out.println(
					"	</tbody>\n" 
				+ "</table>"
				+	"	</body>\n" 
				+ "</html>");
		//@formatter:on

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

			HttpServletResponse resp = Dust.access(data, MindAccess.Peek, null, ServletData.Response);
			resp.setContentType(CONTENT_HTML);
			
			PrintWriter w = getWriter(data);
			
			w.print("<html lang=\"en-US\">\n"
					+ "<head>\n"
					+ "<title>Data content of report " + rep.get("fxo_id") + "</title>\n"
					+ "\n"
					+ "<style>\n"
					+ "table, th, td {\n"
					+ "  border: 1px solid black;\n"
					+ "}"
					+ "</style>\n"
					+ "</head>\n"
					+ "<body>");
			
			w.println("<h1>Report info</h1> <table >\n" 
					+ "	<thead>\n" 
					+ "		<tr>\n" 
					+ "			<th>Key</th>\n" 
					+ "			<th>Value</th>\n"
					+ "		</tr>\n" 
					+ "	</thead>\n" 
					+ "	<tbody>");
			
			for ( Object ri : rep.entrySet() ) {
				Map.Entry re = (Entry) ri;
				w.println("<tr><td>" + re.getKey() + "</td><td>" + re.getValue() + "</td></tr>");
			}
			
			w.println(
					"	</tbody>\n" 
				+ "</table>");
		
						
			File f;
			
			f = filings.getReport(url, lDir, XbrlReportType.ContentVal);

			try (BufferedReader br = new BufferedReader(new FileReader(f))) {
				boolean first = true;
				
				for (String line; (line = br.readLine()) != null;) {
					if ( first ) {
						first = false;
						
						String tl = line.replaceAll("\t", "</th><th>");
						
						w.println("<h1>All values</h1> <table style=\"width:100%\">\n	<thead>\n"); 
						
						w.println("<tr><th>" + tl + "</th></tr>");
						
						w.println("</thead>\n	<tbody>");

					} else {
						String tl = line.replaceAll("\t", "</td><td>");
						w.println("<tr><td>" + tl + "</td></tr>");
					}
				}
				if ( !first ) {
					w.println(
							"	</tbody>\n" 
						+ "</table>");
				}
			}
					
			f = filings.getReport(url, lDir, XbrlReportType.ContentTxt);

			try (BufferedReader br = new BufferedReader(new FileReader(f))) {
				boolean first = true;
				
				for (String line; (line = br.readLine()) != null;) {
					if ( first ) {
						first = false;
						String tl = line.replaceAll("\t", "</th><th>");
											
						w.println("<h1>Text values</h1> <table style=\"width:100%\">\n	<thead>\n"); 
						
						w.println("<tr><th>" + tl + "</th></tr>");
						
						w.println("</thead>\n	<tbody>");
					} else {
						String tl = line.replaceAll("\t", "</td><td>");
						w.println("<tr><td>" + tl + "</td></tr>");
					}
				}
				
				if ( !first ) {
					w.println(
							"	</tbody>\n" 
						+ "</table>");
				}
			}
			
			w.println(
					"	</body>\n" 
				+ "</html>");

			w.flush();
			
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
			String fn = (String) rep.get("fxo_id");

			HttpServletResponse resp = Dust.access(data, MindAccess.Peek, null, ServletData.Response);

			switch ( type ) {
			case "package_url":
				fn += ".zip";
				resp.setHeader("Content-Disposition", "attachment; filename=" + fn);
				cType = CONTENT_ZIP + "; filename=" + fn;
				repType = XbrlReportType.Zip;
				break;
			case "json_url":
				cType = CONTENT_JSON;
				repType = XbrlReportType.Json;
				break;
			case "csv":
				String ct = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "ct");

				fn += ("_" + ct + ".csv");
				resp.setHeader("Content-Disposition", "attachment; filename=" + fn);
				cType = CONTENT_CSV + "; filename=" + fn;
				
				repType = "txt".equals(ct) ? XbrlReportType.ContentTxt : XbrlReportType.ContentVal;

				url = Dust.access(rep, MindAccess.Peek, null, "package_url");
//				File f = filings.getReport(url, lDir, XbrlReportType.Data);
//				try (BufferedReader br = new BufferedReader(new FileReader(f))) {
//					PrintWriter w = getWriter(data);
//
//					for (String line; (line = br.readLine()) != null;) {
//						if ( line.startsWith(ct) ) {
//							w.println(line.substring(line.indexOf("\t") + 1));
//						}
//					}
//
//					w.flush();
//				}
				break;
//				return;
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
