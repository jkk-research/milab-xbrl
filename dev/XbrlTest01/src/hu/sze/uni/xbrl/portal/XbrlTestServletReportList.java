package hu.sze.uni.xbrl.portal;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.MVEL;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts.MindAccess;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.uni.http.DustHttpServlet;
import hu.sze.uni.xbrl.XbrlFilingManager;
import hu.sze.uni.xbrl.portal.XbrlTestPortalConsts.ListColumns;

@SuppressWarnings("rawtypes")
class XbrlTestServletReportList extends DustHttpServlet {
	private static final long serialVersionUID = 1L;

	public static class ExprSvc {
		Object compExpr;
		Map<String, ArrayList<String[]>> allFactsByRep;

		Map currRep;
		ArrayList<String[]> repFacts = new ArrayList<>();

		public ExprSvc(Map<String, ArrayList<String[]>> allFactsByRep, Map currRep, String expr) {
			this.allFactsByRep = allFactsByRep;
			this.currRep = currRep;
			compExpr = DustUtils.isEmpty(expr) ? null : MVEL.compileExpression(expr);
		}

		public boolean test() {
			if ( null == compExpr ) {
				return true;
			}

			String id = (String) currRep.get("Report");
			repFacts = allFactsByRep.get(id);

			return (boolean) MVEL.executeExpression(compExpr, this, currRep);
		}

		public Object headerField(String name) {
			return currRep.get(name);
		}

		public boolean hasFact(String taxonomy, String concept) {
			if ( null != repFacts ) {
				for (String[] rf : repFacts) {
					if ( DustUtils.isEqual(taxonomy, rf[1]) && DustUtils.isEqual(concept, rf[2]) ) {
						return true;
					}
				}
			}
			return false;
		}
	}

	private XbrlFilingManager filings;
//		ArrayList<String[]> allFacts = new ArrayList<>();
	Map<String, ArrayList<String[]>> allFactsByRep = new HashMap<>();

	public XbrlTestServletReportList(XbrlFilingManager filings, Map<String, ArrayList<String[]>> allFactsByRep) {
		this.filings = filings;
		this.allFactsByRep = allFactsByRep;
	}

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

		String expr = "Country.equalsIgnoreCase(\"hu\") && hasFact(\"ifrs-full\", \"AdjustmentsForUndistributedProfitsOfInvestmentsAccountedForUsingEquityMethod\") ";
		Map rep = new HashMap();

		ExprSvc exprFilter = new ExprSvc(allFactsByRep, rep, expr);

		for (Map repSrc : filings.getReportData().values()) {
			rep = ListColumns.load(repSrc, rep);

			if ( !exprFilter.test() ) {
				continue;
			}

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
}