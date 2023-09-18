package hu.sze.uni.xbrl.portal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.mvel2.MVEL;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts;
import hu.sze.milab.dust.DustConsts.MindAccess;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.xbrl.tools.XbrlToolsCurrencyConverter;
import hu.sze.uni.http.DustHttpServlet;
import hu.sze.uni.xbrl.XbrlConsts.XbrlReportType;
import hu.sze.uni.xbrl.XbrlFilingManager;
import hu.sze.uni.xbrl.XbrlUtilsCounter;
import hu.sze.uni.xbrl.portal.XbrlTestPortalConsts.ListColumns;

@SuppressWarnings({ "rawtypes" })
class XbrlTestServletReportList extends DustHttpServlet {
	private static final long serialVersionUID = 1L;

	public static class ExprSvc {
		private static final String EXPR_REP_DATA = "report";
		private static final String EXPR_REP_SRC = "repSrc";
		private static final String EXPR_FACT = "fact";

		private static final String EXPR_PROC_STATE = "ProcState";
		private static final String EXPR_PROC_STATE_FACT = "Fact";
		private static final String EXPR_PROC_STATE_REPEND = "RepEnd";
		private static final String EXPR_PROC_STATE_PROCEND = "ProcEnd";

		Object headExpr;
		Object factExpr;
		Object proc;
		Map<String, Object> varCtx = new HashMap<>();

		public ExprSvc(String strHeadExpr, String strFactExpr, String strProc) {
			headExpr = DustUtils.isEmpty(strHeadExpr) ? null : MVEL.compileExpression(strHeadExpr);
			factExpr = DustUtils.isEmpty(strFactExpr) ? null : MVEL.compileExpression(strFactExpr);
			proc = DustUtils.isEmpty(strProc) ? null : MVEL.compileExpression(strProc);
		}

		public boolean test(Map src, Map currRep, DustUtils.TableReader tr, Iterable<String[]> repFacts) {
			if ( (null == headExpr) && (null == factExpr) ) {
				return true;
			}

			varCtx.put(EXPR_REP_SRC, src);
			varCtx.put(EXPR_REP_DATA, currRep);

			boolean match = (null == headExpr) ? true : (boolean) MVEL.executeExpression(headExpr, this, varCtx);

			if ( match && (null != factExpr) ) {
				match = false;

				if ( null != repFacts ) {
					Map currFact = new HashMap<>();
					varCtx.put(EXPR_FACT, currFact);

					for (String[] rf : repFacts) {
//						if ( null == tr ) {
//							tr = new DustUtils.TableReader(rf);
//						} else {
						tr.getUntil(rf, currFact, null);

						if ( (boolean) MVEL.executeExpression(factExpr, this, varCtx) ) {
							if ( null == proc ) {
								return true;
							} else {
								match = true;
								varCtx.put(EXPR_PROC_STATE, EXPR_PROC_STATE_FACT);
								MVEL.executeExpression(proc, this, varCtx);
							}
						}

						currFact.clear();
//						}

					}
				}

				if ( null != proc ) {
					varCtx.put(EXPR_PROC_STATE, EXPR_PROC_STATE_REPEND);
					MVEL.executeExpression(proc, this, varCtx);
				}
			}

			return match;
		}

		public boolean postFilter(Map src, Map currRep, Map currFact) {
			varCtx.put(EXPR_REP_SRC, src);
			varCtx.put(EXPR_REP_DATA, currRep);

			varCtx.put(EXPR_FACT, currFact);

			return (boolean) MVEL.executeExpression(factExpr, this, varCtx);
		}

		public void count(String str) {
			XbrlUtilsCounter cntr = (XbrlUtilsCounter) varCtx.get("Counter");
			if ( null == cntr ) {
				varCtx.put("Counter", cntr = new XbrlUtilsCounter(true));
			}
			cntr.add(str);
		}

		public void optClose() {
			if ( null != proc ) {
				varCtx.put(EXPR_PROC_STATE, EXPR_PROC_STATE_PROCEND);
				MVEL.executeExpression(proc, this, varCtx);
			}

			XbrlUtilsCounter cntr = (XbrlUtilsCounter) varCtx.get("Counter");
			if ( null != cntr ) {
				cntr.dump("Counted in proc");
			}
		}
	}

	private XbrlFilingManager filings;
//		ArrayList<String[]> allFacts = new ArrayList<>();
//	Map<String, ArrayList<String[]>> allFactsByRep = new HashMap<>();
//	Map<String, DustUtils.TableReader> headers = new HashMap<>();

	public XbrlTestServletReportList(XbrlTestPortal portal) {
		this.filings = portal.filings;
//		this.allFactsByRep = portal.allFactsByRep;
//		this.headers = portal.headers;
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

		String mode = Dust.access(data, MindAccess.Peek, "ShowList", ServletData.Parameter, "mode");
		boolean csvOut = !"ShowList".equals(mode);
		DustUtils.TableReader trMax = null;

		String exprHead = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "exprHead");
		String exprFact = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "exprFact");
		String proc = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "exprProc");
		String sort = Dust.access(data, MindAccess.Peek, null, ServletData.Parameter, "sort");
		String repColStr = Dust.access(data, MindAccess.Peek, "Report", ServletData.Parameter, "repCols");

		String pageSize = Dust.access(data, MindAccess.Peek, "500", ServletData.Parameter, "page[size]");
		String pageNum = Dust.access(data, MindAccess.Peek, "1", ServletData.Parameter, "page[number]");

		boolean inEUR = "on".equals(Dust.access(data, MindAccess.Peek, "off", ServletData.Parameter, "inEUR"));

		boolean loadFacts = !DustUtils.isEmpty(exprFact);

		Map rep = new HashMap();

		ExprSvc exprSvc = null;
		String exprErr = null;

		ArrayList<Map> res = new ArrayList<>();

		DustUtils.TableReader tr = null;
		Iterable<String[]> repFacts = null;

		Set<Map> loadErr = new HashSet<>();

		try {
			exprSvc = new ExprSvc(exprHead, exprFact, proc);

			for (Map repSrc : filings.getReportData().values()) {
				rep = ListColumns.load(repSrc, rep);
				String id = (String) rep.get("Report");
				tr = filings.getTableReader(id);

				if ( null == tr ) {
					loadErr.add(rep);
					continue;
				}

				if ( loadFacts ) {
					repFacts = filings.getFacts(id);
				}

				if ( exprSvc.test(repSrc, rep, tr, repFacts) ) {
					res.add(rep);

					if ( csvOut ) {
						if ( (null == trMax) || (trMax.getSize() < tr.getSize()) ) {
							trMax = tr;
						}
					}

					rep = null;
				}
			}

			exprSvc.optClose();
		} catch (Throwable t) {

			t.printStackTrace();

			StringBuilder sb = new StringBuilder("<h1>Expression error</h1>");
			sb.append("\n<ul>").append(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				sb.append("\n  <li>").append(ste.toString()).append("</li>");
				if ( ste.getClassName().startsWith("javax.servlet") ) {
					break;
				}
			}
			sb.append("\n</ul>");

			exprErr = sb.toString();
			csvOut = false;
		}

		if ( !DustUtils.isEmpty(sort) && (res.size() > 1) ) {
			Comparator<Map> cmp = new DustUtils.MapComparator(sort, ",");
			res.sort(cmp);
		}

		HttpServletResponse resp = Dust.access(data, MindAccess.Peek, null, ServletData.Response);

		if ( csvOut ) {
			boolean filtered = "Filtered".equals(mode) && (null != exprSvc.factExpr);
			String fn = Dust.access(data, MindAccess.Peek, "ReportData", ServletData.Parameter, "fName");
			fn += ("_" + mode + "_" + new SimpleDateFormat(DustConsts.FMT_TIMESTAMP).format(new Date()) + ".csv");
			resp.setHeader("Content-Disposition", "attachment; filename=" + fn);
			resp.setContentType(CONTENT_CSV + "; filename=" + fn);
			Map currFact = new HashMap<>();

			XbrlToolsCurrencyConverter cCvt = inEUR ? new XbrlToolsCurrencyConverter("EUR", "params/excRate_5yr_v2.csv", ";") : null;
			Map<String, Object> mapFacts = new HashMap<>();

			PrintWriter out = getWriter(data);

			String[] repCols = {};
			int repColCount = 0;

			if ( !DustUtils.isEmpty(repColStr) ) {
				repCols = repColStr.split(",");
				repColCount = repCols.length;
				for (int i = 0; i < repColCount; ++i) {
					repCols[i] = repCols[i].trim();
					out.print(repCols[i]);
					out.print("\t");
				}
			}

			if ( "Text".equals(mode) ) {
				trMax.writeHeadPart(out, "\t", trMax.getColIdx("OrigValue"));
				out.println("\tLanguage\tValue");
			} else {
				if ( inEUR ) {
					trMax.writeHeadPart(out, "\t", Integer.MAX_VALUE);
					out.println("\tcvtEurValue\tcvtEurStatus\tcvtEurCount");
				} else {
					trMax.writeHead(out, "\t");
				}
			}

			for (Map r : res) {
				String id = (String) r.get("Report");

				tr = filings.getTableReader(id);
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

				StringBuilder sbRepHead = null;
				for (int i = 0; i < repColCount; ++i) {
					sbRepHead = DustUtils.sbAppend(sbRepHead, "\t", true, r.get(repCols[i]));
				}
				String repHead = DustUtils.toString(sbRepHead);

				if ( "Text".equals(mode) ) {
					Map repSrc = filings.getReportData().get(id);
					File fTxt = filings.getReport(repSrc, XbrlReportType.ContentTxt, true);

					if ( fTxt.isFile() ) {
						boolean firstLine = true;
						try (BufferedReader br = new BufferedReader(new FileReader(fTxt))) {
							for (String line; (line = br.readLine()) != null;) {
								if ( firstLine ) {
									firstLine = false;
									continue;
								}

								if ( 0 < diff ) {
									int insertIdx = 0;
									for (int i = 0; i < ovi; ++i) {
										insertIdx = line.indexOf("\t", insertIdx + 1);
									}
									line = line.substring(0, insertIdx) + fill + line.substring(insertIdx);
								}
								out.print(repHead);
								out.print("\t");
								out.println(line);
							}
						}
					}
				} else {

					repFacts = filings.getFacts(id);

					if ( null != repFacts ) {

						for (String[] rf : repFacts) {
							if ( filtered ) {
								tr.getUntil(rf, currFact, null);

								if ( !exprSvc.postFilter(r, null, currFact) ) {
//							if ( !(boolean) MVEL.executeExpression(exprSvc.factExpr, this, currFact) ) {
									continue;
								}

								currFact.clear();
							}

							for (int i = 0; i < repColCount; ++i) {
								if ( 0 < i ) {
									out.print("\t");
								}
								Object rv = r.get(repCols[i]);
								if ( null != rv ) {
									out.print(rv);
								}
							}

							for (int i = 0; i < rf.length; ++i) {
								out.print("\t");
								if ( i == ovi ) {
									out.print(fill);
								}
								out.print(rf[i]);
							}

							for (int i = rf.length; i < tr.getSize(); ++i) {
								out.print("\t");
							}

							if ( inEUR ) {
								mapFacts.clear();
								tr.get(rf, mapFacts);
								cCvt.optConvert(mapFacts);
								out.print("\t" + mapFacts.get("cvtValue") + "\t" + mapFacts.get("cvtStatus") + "\t" + mapFacts.get("cvtCount"));
							}

							out.println();
						}
					}
				}
			}

		} else {
			resp.setContentType(CONTENT_HTML);
			PrintWriter out = getWriter(data);

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
					} else if ( -1 != (idx = optGetIdxAfter(line, "name=\"exprProc\">", proc)) ) {
						str = proc;
					} else if ( -1 != (idx = optGetIdxAfter(line, "name=\"sort\" ", sort)) ) {
						str = "value=\"" + sort + "\" ";
					} else if ( -1 != (idx = optGetIdxAfter(line, "name=\"repCols\" ", repColStr)) ) {
						str = "value=\"" + repColStr + "\" ";
					} else if ( -1 != (idx = optGetIdxAfter(line, "name=\"page[size]\" ", pageSize)) ) {
						str = "value=\"" + pageSize + "\" ";
					} else if ( -1 != (idx = optGetIdxAfter(line, "name=\"page[number]\" ", pageNum)) ) {
						str = "value=\"" + pageNum + "\" ";
					} else if ( -1 != (idx = optGetIdxAfter(line, "name=\"inEUR\" ", pageNum)) ) {
						if ( inEUR ) {
							str = "checked ";
						}
					} else if ( -1 != (idx = optGetIdxAfter(line, "Row count: ", " ")) ) {
						str = " " + res.size();
					}

					if ( -1 != idx ) {
						line = insert(line, idx, str);
					}

					out.println(line);
				}
			}

			if ( null != exprErr ) {
				out.println(exprErr);
			} else {
//				out.println("Row count: " + res.size());

				out.println("<table>\n	<thead>\n	<tr>\n");
				out.println("			<th>#</th>\n");

				for (ListColumns lc : ListColumns.values()) {
					out.println("			<th>" + lc + "</th>\n");
				}
				out.println(" </tr>\n	</thead>\n <tbody>");

				int ps = Integer.parseInt(pageSize);
				int pn = Integer.parseInt(pageNum);

				int end = pn * ps + 1;
				int start = end - ps;
				int row = 0;

				for (Map r : res) {
					++row;
					if ( row < start ) {
						continue;
					}
					if ( row >= end ) {
						break;
					}

					out.println("<tr>\n			<td>" + row + "</td>\n");
					boolean err = loadErr.contains(r);

					for (ListColumns lc : ListColumns.values()) {
						Object val = r.get(lc.name());

						switch ( lc ) {
						case CsvVal:
						case CsvTxt:
							if ( err ) {
								val = "-";
							}
							break;
						case Report:
							val = err ? "Load error" : ("<a href=\"/report/" + val + "\">" + val + "</a>");
							break;
						default:
							break;
						}

						out.println("			<td>" + val + "</td>\n");
					}

					out.println(" </tr>\n");
				}

				out.println("	</tbody>\n</table>\n");
			}
			out.println("</body>\n</html>");
		}
	}
}