package hu.sze.uni.xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.mvel2.MVEL;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.uni.http.DustHttpServerJetty;
import hu.sze.uni.http.DustHttpServlet;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlTestPortal implements XbrlConsts {

	private static final Pattern PT_DATE_ONLY = Pattern.compile("(\\d+-\\d+-\\d+).*");

	enum ListColumns {
		Country("country"), PeriodEnd("period_end"), Entity(XbrlFilingManager.ENTITY_NAME), DateAdded("date_added"), Report(XbrlFilingManager.REPORT_ID), CsvVal(XbrlFilingManager.REPORT_ID),
		CsvTxt(XbrlFilingManager.REPORT_ID), Zip("package_url"), Json("json_url"), ChkErr("error_count"), ChkWarn("warning_count"), ChkInc("inconsistency_count");

		public final String colName;

		private ListColumns(String colName) {
			this.colName = colName;
		}

		static Map load(Map from, Map to) {
			if ( null == to ) {
				to = new TreeMap();
			} else {
				to.clear();
			}

			Object repId = from.get(XbrlFilingManager.REPORT_ID);

			for (ListColumns lc : values()) {
				Object val = from.get(lc.colName);

				switch ( lc ) {
				case DateAdded:
					Matcher m = PT_DATE_ONLY.matcher((String) val);
					if ( m.matches() ) {
						val = m.group(1);
					}
					break;
				case CsvVal:
				case CsvTxt:
					val = "<a href=\"/bin?type=csv&ct=" + lc + "&id=" + val + "\">" + lc + "</a>";
					break;
				case Zip:
				case Json:
					val = DustUtils.isEmpty((String)val) ? " - " : "<a href=\"/bin?type=" + lc.colName + "&id=" + repId + "\">" + lc + "</a>";
					break;
				default:
					break;
				}

				to.put(lc.name(), val);
			}

			return to;
		}

	}

	private File dataRoot;
	private XbrlFilingManager filings;

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
			//@formatter:on

			out.println("<table>\n	<thead>\n	<tr>\n");
			for (ListColumns lc : ListColumns.values()) {
				out.println("			<th>" + lc + "</th>\n");
			}
			out.println(" </tr>\n	</thead>\n <tbody>");
			
			Map rep = null;
			
			Object expr = MVEL.compileExpression("Country.equalsIgnoreCase(\"hu\") ");
			
			for (Map repSrc : filings.reportData.values()) {
				rep = ListColumns.load(repSrc, rep);
				
//				Boolean test = (Boolean) MVEL.executeExpression(expr, rep);
//				
//				if ( !test ) {
//					continue;
//				}

				
				out.println("<tr>\n");

				for (ListColumns lc : ListColumns.values()) {
					Object val = rep.get(lc.name());
					
					switch ( lc ) {
					case Report:
						val = "<a href=\"/report/" + val + "\">" + val + "</a>";
						break;
						default:
							break;
					}
					
					out.println("			<td>" + val + "</td>\n");
				}

				out.println(" </tr>\n");
			}

			out.println("	</tbody>\n</table>\n</body>\n</html>");

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

			w.print("<html lang=\"en-US\">\n" + "<head>\n" + "<title>Data content of report " + rep.get("fxo_id") + "</title>\n"
					+ "<style>\n" + "table, th, td {\n" + "  border: 1px solid black;\n"
					+ "}" + "</style>\n" + "</head>\n" + "<body>");

			w.println("<h1>Report info</h1> <table >\n	<thead>\n		<tr>\n			<th>Key</th>\n			<th>Value</th>\n		</tr>\n	</thead>\n	<tbody>");

			for (Object ri : rep.entrySet()) {
				Map.Entry re = (Entry) ri;
				w.println("<tr><td>" + re.getKey() + "</td><td>" + re.getValue() + "</td></tr>");
			}

			w.println("	</tbody>\n" + "</table>");

			File f;

			f = filings.getReport(url, lDir, XbrlReportType.ContentVal);

			try (BufferedReader br = new BufferedReader(new FileReader(f))) {
				boolean first = true;

				for (String line; (line = br.readLine()) != null;) {
					if ( first ) {
						first = false;

						w.println("<h1>All values</h1> <table style=\"width:100%\">\n	<thead>\n");

						String tl = line.replaceAll("\t", "</th><th>");
						w.println("<tr><th>" + tl + "</th></tr>");

						w.println("</thead>\n	<tbody>");

					} else {
						String tl = line.replaceAll("\t", "</td><td>");
						w.println("<tr><td>" + tl + "</td></tr>");
					}
				}
				if ( !first ) {
					w.println("	</tbody>\n" + "</table>");
				}
			}

			f = filings.getReport(url, lDir, XbrlReportType.ContentTxt);

			try (BufferedReader br = new BufferedReader(new FileReader(f))) {
				boolean first = true;

				for (String line; (line = br.readLine()) != null;) {
					if ( first ) {
						first = false;

						w.println("<h1>Text values</h1> <table style=\"width:100%\">\n	<thead>\n");

						String tl = line.replaceAll("\t", "</th><th>");
						w.println("<tr><th>" + tl + "</th></tr>");

						w.println("</thead>\n	<tbody>");
					} else {
						String tl = line.replaceAll("\t", "</td><td>");
						w.println("<tr><td>" + tl + "</td></tr>");
					}
				}

				if ( !first ) {
					w.println("	</tbody>\n" + "</table>");
				}
			}

			w.println("	</body>\n" + "</html>");

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

				repType = ListColumns.CsvTxt.name().equals(ct) ? XbrlReportType.ContentTxt : XbrlReportType.ContentVal;

				url = Dust.access(rep, MindAccess.Peek, null, "package_url");
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
