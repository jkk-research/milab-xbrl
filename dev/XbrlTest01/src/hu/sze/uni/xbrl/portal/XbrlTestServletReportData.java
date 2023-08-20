package hu.sze.uni.xbrl.portal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts.MindAccess;
import hu.sze.uni.http.DustHttpServlet;
import hu.sze.uni.xbrl.XbrlConsts.XbrlReportType;
import hu.sze.uni.xbrl.XbrlFilingManager;

@SuppressWarnings("rawtypes")
class XbrlTestServletReportData extends DustHttpServlet {
	private static final long serialVersionUID = 1L;
	
	private XbrlFilingManager filings;

	public XbrlTestServletReportData(XbrlFilingManager filings) {
		this.filings = filings;
	}


	@Override
	protected void processRequest(Map data) throws Exception {
		Dust.dumpObs("get report data", data);

		String id = Dust.access(data, MindAccess.Peek, null, ServletData.Command);

		Map rep = filings.getReportData().get(id);
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
}