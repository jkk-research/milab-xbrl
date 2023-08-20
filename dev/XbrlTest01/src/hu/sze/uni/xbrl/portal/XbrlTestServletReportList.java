package hu.sze.uni.xbrl.portal;

import java.io.PrintWriter;
import java.util.Map;

import org.mvel2.MVEL;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts.MindAccess;
import hu.sze.uni.http.DustHttpServlet;
import hu.sze.uni.xbrl.XbrlFilingManager;
import hu.sze.uni.xbrl.portal.XbrlTestPortalConsts.ListColumns;

@SuppressWarnings("rawtypes")
class XbrlTestServletReportList extends DustHttpServlet {
		private static final long serialVersionUID = 1L;
		
		private XbrlFilingManager filings;

		public XbrlTestServletReportList(XbrlFilingManager filings) {
			this.filings = filings;
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
			
			Map rep = null;
			
			Object expr = MVEL.compileExpression("Country.equalsIgnoreCase(\"hu\") ");
			
			for (Map repSrc : filings.getReportData().values()) {
				rep = ListColumns.load(repSrc, rep);
				
				Boolean test = (Boolean) MVEL.executeExpression(expr, rep);
				
				if ( !test ) {
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