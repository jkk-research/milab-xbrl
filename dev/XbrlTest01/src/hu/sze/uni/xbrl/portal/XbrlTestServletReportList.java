package hu.sze.uni.xbrl.portal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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
		Object headExpr;
		Object factExpr;

		XbrlTestServletReportList repList;

		public ExprSvc(XbrlTestServletReportList repList, String strHeadExpr, String strFactExpr) {
			this.repList = repList;

			headExpr = DustUtils.isEmpty(strHeadExpr) ? null : MVEL.compileExpression(strHeadExpr);
			factExpr = DustUtils.isEmpty(strFactExpr) ? null : MVEL.compileExpression(strFactExpr);
		}

		public boolean test(Map currRep) {
			if ( (null == headExpr) && (null == factExpr) ) {
				return true;
			}

			boolean match = (null == headExpr) ? true : (boolean) MVEL.executeExpression(headExpr, this, currRep);

			if ( match && (null != factExpr) ) {
				String id = (String) currRep.get("Report");

				match = false;

//				Map mapFiling = filings.getReportData().get(id);
//				String lDir = Dust.access(mapFiling, MindAccess.Peek, null, XbrlFilingManager.LOCAL_DIR);
//				File f = new File(repoRoot, lDir + "/Report_Val.csv");
//
//				if ( (null != f ) && f.isFile() ) {
//					try (BufferedReader br = new BufferedReader(new FileReader(f))) {
//						DustUtils.TableReader tr = null;
//						Map currFact = new HashMap<>();
//
//						for (String line; (line = br.readLine()) != null;) {
//							if ( !DustUtils.isEmpty(line) ) {
//								String[] data = line.split("\t");
//
//								if ( null == tr ) {
//									tr = new DustUtils.TableReader(data);
//								} else {
//									tr.getUntil(data, currFact, null);
//									match = (boolean) MVEL.executeExpression(factExpr, this, currFact);
//
//									if ( match ) {
//										return true;
//									}
//
//									currFact.clear();
//								}
//							}
//						}
//					}
//				}

				ArrayList<String[]> repFacts = repList.allFactsByRep.get(id);

				if ( null != repFacts ) {
					DustUtils.TableReader tr = repList.headers.get(id);
					Map currFact = new HashMap<>();

					for (String[] rf : repFacts) {
						if ( null == tr ) {
							tr = new DustUtils.TableReader(rf);
						} else {
							tr.getUntil(rf, currFact, null);

							match = (boolean) MVEL.executeExpression(factExpr, this, currFact);

							if ( match ) {
								return true;
							}

							currFact.clear();
						}

					}
				}

			}

			return match;
		}

	}

	private XbrlFilingManager filings;
//		ArrayList<String[]> allFacts = new ArrayList<>();
	Map<String, ArrayList<String[]>> allFactsByRep = new HashMap<>();
	Map<String, DustUtils.TableReader> headers = new HashMap<>();

	public XbrlTestServletReportList(XbrlTestPortal portal) {
		this.filings = portal.filings;
		this.allFactsByRep = portal.allFactsByRep;
		this.headers = portal.headers;
	}

	String insert(String line, int at, String str) {
		return line.substring(0, at) + str + line.substring(at);
	}

	int optGetIdxAfter(String line, String loc, String val) {
		if ( !DustUtils.isEmpty(val) ) {
			int idx = line.indexOf(loc);
			if ( -1 != idx ) {
				return idx + loc.length();
			}
		}

		return -1;
	}

	@Override
	protected void processRequest(Map data) throws Exception {
		Dust.dumpObs("get report list", data);

		String mode = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "mode");
		boolean csvOut = "Filtered".equals(mode) || "All".equals(mode);
		DustUtils.TableReader trMax = null;

		String exprHead = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "exprHead");
		String exprFact = Dust.access(data, MindAccess.Peek, "(null != Concept) && Concept.contains(\"Emission\") ", ServletData.Parameter, "exprFact");
		String sort = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "sort");

		Map rep = new HashMap();

		ExprSvc exprFilter = new ExprSvc(this, exprHead, exprFact);

		ArrayList<Map> res = new ArrayList<>();

		for (Map repSrc : filings.getReportData().values()) {
			rep = ListColumns.load(repSrc, rep);

			if ( exprFilter.test(rep) ) {
				res.add(rep);

				if ( csvOut ) {
					Object id = rep.get("Report");
					DustUtils.TableReader tr = headers.get(id);

					if ( (null == trMax) || (trMax.getSize() < tr.getSize()) ) {
						trMax = tr;
					}
				}
				rep = null;
			}
		}

		if ( !DustUtils.isEmpty(sort) && (res.size() > 1) ) {
			Comparator<Map> cmp = new DustUtils.MapComparator(sort, ",");
			res.sort(cmp);
		}

		PrintWriter out = getWriter(data);

		if ( csvOut ) {
			HttpServletResponse resp = Dust.access(data, MindAccess.Peek, null, ServletData.Response);

			boolean filtered = "Filtered".equals(mode) && (null != exprFilter.factExpr);
			String fn = Dust.access(data, MindAccess.Peek, "ReportData", ServletData.Parameter, "fName");
			fn += ("_" + mode + ".csv");
			resp.setHeader("Content-Disposition", "attachment; filename=" + fn);
			resp.setContentType(CONTENT_CSV + "; filename=" + fn);
			Map currFact = new HashMap<>();

			out.print("Report\t");

			trMax.writeHead(out, "\t");

			for (Map r : res) {
				String id = (String) r.get("Report");

				ArrayList<String[]> repFacts = allFactsByRep.get(id);

				if ( (null != repFacts) && !repFacts.isEmpty() ) {
					DustUtils.TableReader tr = headers.get(id);
					int ts = tr.getSize();
					int diff = trMax.getSize() - ts;
					int ovi = -1;

					StringBuilder fill = null;
					if ( 0 < diff ) {
						fill = new StringBuilder();
						for (int i = 0; i < diff; ++i) {
							fill.append("\t");
						}
						ovi = tr.getColIdx("OrigValue");
					}

					for (String[] rf : repFacts) {
						if ( filtered ) {
							tr.getUntil(rf, currFact, null);

							if ( !(boolean) MVEL.executeExpression(exprFilter.factExpr, this, currFact) ) {
								continue;
							}

							currFact.clear();
						}

						out.print(id);

						for (int i = 0; i < rf.length; ++i) {
							out.print("\t");
							if ( i == ovi ) {
								out.print(fill);
							}
							out.print(rf[i]);
						}

						out.println();
					}
				}
			}

		} else {

			File fForm = new File("ReportFilterForm.html");
			try (BufferedReader br = new BufferedReader(new FileReader(fForm))) {
				for (String line; (line = br.readLine()) != null;) {
					if ( line.contains(" CUT ") ) {
						break;
					}

					int idx;
					String str = null;

					if ( -1 != (idx = optGetIdxAfter(line, "name=\"exprHead\">", exprHead)) ) {
						str = exprHead;
					} else if ( -1 != (idx = optGetIdxAfter(line, "name=\"exprFact\">", exprFact)) ) {
						str = exprFact;
					} else if ( -1 != (idx = optGetIdxAfter(line, "name=\"sort\" ", sort)) ) {
						str = "value=\"" + sort + "\" ";
					}

					if ( -1 != idx ) {
						line = insert(line, idx, str);
					}

					out.println(line);
				}
			}

			out.println("<table>\n	<thead>\n	<tr>\n");
			for (ListColumns lc : ListColumns.values()) {
				out.println("			<th>" + lc + "</th>\n");
			}
			out.println(" </tr>\n	</thead>\n <tbody>");

			for (Map r : res) {
				out.println("<tr>\n");

				for (ListColumns lc : ListColumns.values()) {
					Object val = r.get(lc.name());

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

			out.println("	</tbody>\n</table>\n Row count: " + res.size() + "</body>\n</html>");
		}
	}
}