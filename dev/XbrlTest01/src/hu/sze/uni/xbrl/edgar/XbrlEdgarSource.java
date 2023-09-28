package hu.sze.uni.xbrl.edgar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.simple.parser.JSONParser;
import org.mvel2.MVEL;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsData;
import hu.sze.milab.dust.utils.DustUtilsFile;
import hu.sze.milab.dust.utils.DustUtilsXml;
import hu.sze.uni.xbrl.XbrlReportLoaderDomBase;
import hu.sze.uni.xbrl.XbrlUtils;
import hu.sze.uni.xbrl.XbrlUtilsCounter;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlEdgarSource implements XbrlEdgarConsts {

	final File edgarRoot;
	JSONParser parser = new JSONParser();
	ThreadLocal<JSONParser> jp = new ThreadLocal<JSONParser>() {
		@Override
		protected JSONParser initialValue() {
			return new JSONParser();
		}
	};

	DocumentBuilderFactory dbf;

//	Map companies;

	XbrlUtilsCounter dc = new XbrlUtilsCounter(true);
	DustUtils.ProcessMonitor pm;
	File fSubmissionIndex;
	File fFactRoot;
	File fSubmissionRoot;
	File fReportRoot;

	private static Long tsLastDownload = 0L;

	public XbrlEdgarSource(File dataRoot) {
		edgarRoot = new File(dataRoot + "/sources/edgar");

		fFactRoot = new File(edgarRoot, "companyfacts");
		fSubmissionRoot = new File(edgarRoot, "submissions");
		fReportRoot = new File(edgarRoot, "reports");
		fSubmissionIndex = new File(edgarRoot, "SubmissionIndex.csv");
	}

	public static synchronized void safeDownload(String url, File file) throws Exception {
		long ts = System.currentTimeMillis();
		long diff = ts - tsLastDownload;

		if ( 200 > diff ) {
			synchronized (tsLastDownload) {
				tsLastDownload.wait(diff);
			}
		}

		tsLastDownload = System.currentTimeMillis();
		XbrlUtils.download(url, file, EDGAR_APIHDR_USER, EDGAR_APIHDR_ENCODING /* , EDGAR_APIHDR_HOST */);
	}

	public static void main(String[] args) throws Exception {
		Dust.main(args);

		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");

		XbrlEdgarSource edgarSource = new XbrlEdgarSource(dataRoot);

//		edgarSource.loadSubmissions();
//		edgarSource.processSubmissions("\"3711\".equals(company.sic)", "filing.form.contains(\"10-Q\")", "company.name.contains(\"Tesla\") && filing.reportDate.startsWith(\"202\")");
		edgarSource.processSubmissions("\"3711\".equals(company.sic)", null, "company.name.contains(\"Tesla\") && filing.reportDate.startsWith(\"202\")");
//		edgarSource.addPath();

//		edgarSource.delGen();
//		edgarSource.factGen();
//		edgarSource.factStats();

		edgarSource.dc.dump("Summary");
	}

	public File getFiling(Map company, Map filing) throws Exception {
		String formType = (String) filing.get(EdgarSubmissionAtt.form.name());
		String accn = (String) filing.get(EdgarSubmissionAtt.accessionNumber.name());

		String cik = (String) company.get(EdgarHeadFields.cik.name());
		String pathPrefix = (String) company.get(EdgarHeadFields.__PathPrefix.name());
		String docName = (String) filing.get(EdgarSubmissionAtt.primaryDocument.name());

		return getFiling(cik, pathPrefix, formType, accn, docName);
	}

	public File getFiling(String cik, String pathPrefix, String formType, String accn, String docName) throws Exception {
		File f = null;

		boolean docNameMissing = DustUtils.isEmpty(docName);
		if ( docNameMissing ) {
			docName = "index.json";
		}

		File dir = new File(fReportRoot, pathPrefix + "/" + formType + "/" + accn);
		dir.mkdirs();

		f = new File(dir, docName);

		if ( !f.isFile() ) {
			String url = EDGAR_URL_DATA + cik + "/" + accn.replace("-", "") + "/" + docName;
			safeDownload(url, f);
		}

		if ( f.isFile() ) {
			if ( docNameMissing ) {
				docName = selectFileFromJsonIndex(f);
				f = new File(dir, accn + EXT_XML);

				if ( !f.isFile() ) {
					String url = EDGAR_URL_DATA + cik + "/" + accn.replace("-", "") + "/" + docName;
					safeDownload(url, f);
				}

				createSplitCsvFromXml(f, dir, accn, TEXT_CUT_AT);
			} else {
				File fVal = new File(dir, accn + POSTFIX_VAL);
				if ( !fVal.isFile() ) {
					XbrlReportLoaderDomBase.createSplitCsv(f, dir, accn, TEXT_CUT_AT);
				}
			}
		}
		return f;
	}

	Pattern ptSkipFile = Pattern.compile("(R(\\d+)\\.xml)|(.*_(cal|def|lab|pre)\\.xml)");

	public String selectFileFromJsonIndex(File f) throws Exception {
		String selName = null;

		try (Reader fr = new FileReader(f)) {
			Object root = jp.get().parse(fr);
			Collection<Map<String, Object>> items = Dust.access(root, MindAccess.Peek, Collections.EMPTY_LIST, "directory", "item");
			long maxLen = 0;

			for (Map<String, Object> item : items) {
				String name = (String) item.get("name");
				if ( name.endsWith(".xml") ) {
					Matcher m = ptSkipFile.matcher(name);
					if ( !m.matches() ) {
						int l = Integer.parseInt((String) item.get("size"));
						if ( l > maxLen ) {
							maxLen = l;
							selName = name;
						}
					}
				}
			}
		}

		return selName;
	}

	@SuppressWarnings("unused")
	public void createSplitCsvFromXml(File f, File targetDir, String fnPrefix, int textCut) throws Exception {
		Map xbrlElements = new HashMap();

		if ( null == dbf ) {
			dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
		}

		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(f);

		int dimCount = 0;

		Element eRoot = doc.getDocumentElement();
		String[] tt = eRoot.getTagName().split(":");
		String defNS = "";
		
		if ( tt.length > 1 ) {
			defNS = eRoot.getAttribute("xmlns:" + tt[0]);
		}

		String defLang = eRoot.getAttribute("xml:lang");
		Dust.access(xbrlElements, MindAccess.Set, defLang, XbrlElements.DefLang);

		NodeList nl = eRoot.getElementsByTagName("*");
		int nodeCount = nl.getLength();

		for (int idx = 0; idx < nodeCount; ++idx) {
			Element e = (Element) nl.item(idx);
			String tagName = e.getLocalName();

			switch ( tagName ) {
			case "context":
				Map<String, String> cd = new TreeMap<>();

				String ctxId = e.getAttribute("id");
				Dust.access(xbrlElements, MindAccess.Set, cd, XbrlElements.Context, ctxId);

				DustUtilsXml.optLoadTagText(cd, e, defNS, "startDate");
				DustUtilsXml.optLoadTagText(cd, e, defNS, "endDate");
				DustUtilsXml.optLoadTagText(cd, e, defNS, "instant");

				Element eS = null;

				eS = DustUtilsXml.getFirstElement(e, defNS, "segment");
				if ( null == eS ) {
					DustUtilsXml.optLoadTagText(cd, e, defNS, "entity");
					eS = (Element) DustUtilsXml.getFirstElement(e, defNS, "scenario");
				} else {
					Element ee = DustUtilsXml.getFirstElement(e, defNS, "entity");
					String eid = DustUtilsXml.getTagText(ee, defNS, "identifier");
					cd.put("entity", eid);
				}

				if ( null != eS ) {
					NodeList nlS = eS.getChildNodes();
					int dimIdx = 0;
					int dc = nlS.getLength();

					for (int i2 = 0; i2 < dc; ++i2) {
						Node dn = nlS.item(i2);
						if ( dn instanceof Element ) {
							Element m = (Element) dn;
							String dim = m.getAttribute("dimension");
							String dVal = m.getTextContent().trim();
							++dimIdx;
							cd.put("DimName_" + dimIdx, dim);
							cd.put("DimValue_" + dimIdx, dVal);

							if ( dimIdx > dimCount ) {
								dimCount = dimIdx;
							}
						}
					}
				}
				break;
			case "unit":
				String val = DustUtilsXml.getTagText(e, defNS, "unitNumerator");
				if ( null != val ) {
					String denom = DustUtilsXml.getTagText(e, defNS, "unitDenominator");
					val = val + "/" + denom;
				} else {
					val = DustUtilsXml.getTagText(e, defNS, "measure");
				}

				Dust.access(xbrlElements, MindAccess.Set, val, XbrlElements.Unit, e.getAttribute("id"));

				break;
			}
		}

		for (int idx = 0; idx < nodeCount; ++idx) {
			Element e = (Element) nl.item(idx);
			String ctxId = e.getAttribute("contextRef");

			if ( !DustUtils.isEmpty(ctxId) ) {
				String tn = e.getTagName();
				String value = e.getTextContent().trim();
				Map<String, String> ctx = Dust.access(xbrlElements, MindAccess.Peek, null, XbrlElements.Context, ctxId);

				String unitId = e.getAttribute("unitRef");
				String unit = DustUtils.isEmpty(unitId) ? "-" : Dust.access(xbrlElements, MindAccess.Peek, null, XbrlElements.Unit, unitId);

				String dec = e.getAttribute("decimals");
			}
		}
	}

	public void processSubmissions(String filterHead, String filterFiling, String filterDownload) throws Exception {

		pm = new DustUtils.ProcessMonitor("Find companies", 0);

		DustUtilsData.TableReader tr = null;
		Map<String, Object> values = new TreeMap<>();
		Map<String, Object> filing = new TreeMap<>();

		Map<String, Object> ctx = new TreeMap<>();
		ctx.put("company", values);
		ctx.put("filing", filing);

		ArrayList<String> filingLines = new ArrayList<>();

		Object mvelFilterHead = DustUtils.isEmpty(filterHead) ? null : MVEL.compileExpression(filterHead);
		Object mvelFilterFiling = DustUtils.isEmpty(filterFiling) ? null : MVEL.compileExpression(filterFiling);
		Object mvelFilterDownload = DustUtils.isEmpty(filterDownload) ? null : MVEL.compileExpression(filterDownload);

		long fcAll = 0;
		long fcSel = 0;

		try (PrintStream ps = new PrintStream("work/subProcTest.txt"); BufferedReader br = new BufferedReader(new FileReader(fSubmissionIndex))) {
			for (String line; (line = br.readLine()) != null;) {

				pm.step();

				String[] row = line.split("\t");
				if ( null == tr ) {
					tr = new DustUtilsData.TableReader(row);
					ps.println(line);
				} else {
					values.clear();

					for (int i = row.length; i-- > 0;) {
						String sv = row[i];
						if ( (null != sv) && sv.startsWith("\"") ) {
							sv = sv.substring(1, sv.length() - 1);
							sv = sv.replace("\"\"", "\"");
							row[i] = sv;
						}
					}

					tr.get(row, values);
					for (String k : values.keySet()) {
						String sv = (String) values.get(k);

						if ( null != sv ) {
							if ( sv.startsWith("[") ) {
								String[] vv = sv.substring(1, sv.length() - 1).split(",");
								Set<String> s = new TreeSet<>();
								for (String v : vv) {
									s.add(v.trim());
								}
								values.put(k, s);
							}
						}
					}

					long fc = Long.parseLong((String) values.get(EdgarHeadFields.__FilingCount.name()));

					fcAll += fc;

					boolean accepted = true;

					if ( accepted && (null != mvelFilterHead) ) {
						accepted = (Boolean) MVEL.executeExpression(mvelFilterHead, ctx);
					}

					if ( accepted && (null != mvelFilterFiling) ) {
						filingLines.clear();
						File ff = new File(fSubmissionRoot, values.get(EdgarHeadFields.__PathPrefix.name()) + ".csv");

						if ( ff.isFile() ) {
							accepted = false;
							try (BufferedReader brf = new BufferedReader(new FileReader(ff))) {

								DustUtilsData.TableReader trf = null;

								for (String linef; (linef = brf.readLine()) != null;) {
									String[] rowf = linef.split("\t");
									if ( null == trf ) {
										trf = new DustUtilsData.TableReader(rowf);
									} else {
										filing.clear();

										for (int i = rowf.length; i-- > 0;) {
											String sv = rowf[i];
											if ( (null != sv) && sv.startsWith("\"") ) {
												sv = sv.substring(1, sv.length() - 1);
												sv = sv.replace("\"\"", "\"");
												rowf[i] = sv;
											}
										}

										trf.get(rowf, filing);

										if ( (Boolean) MVEL.executeExpression(mvelFilterFiling, ctx) ) {
											accepted = true;
											filingLines.add(linef);

											if ( (null != mvelFilterDownload) && (Boolean) MVEL.executeExpression(mvelFilterDownload, ctx) ) {
												getFiling(values, filing);
											}
										}
									}
								}
							}
						}
					}

					if ( accepted ) {
						ps.println(line);
						fcSel += fc;

						for (String fl : filingLines) {
							ps.println("  " + fl);
						}

						ps.flush();
					}
				}
			}
		}

		System.out.println("All filings: " + fcAll + ", selected: " + fcSel);

		System.out.println(pm);
	}

	public Map processHead(File f) throws Exception {
		FileReader rdrHead = new FileReader(f);
		Map subInfo = (Map) parser.parse(rdrHead);
		rdrHead.close();

		Map companyInfo = new TreeMap<>();

		for (Object he : subInfo.entrySet()) {
			String k = (String) ((Map.Entry) he).getKey();
			Object v = ((Map.Entry) he).getValue();

			if ( "filings".equals(k) ) {
				String name = DustUtils.cutPostfix(f.getName(), ".") + ".csv";
				File fCsv = new File(f.getParentFile(), name);

//				if ( !fCsv.isFile() ) 
				{
					try (PrintWriter pwFilings = new PrintWriter(fCsv)) {

						pwFilings.println(EDGAR_FILING_HEADER);

						processFilings(companyInfo, (Map) ((Map) v).getOrDefault("recent", Collections.EMPTY_MAP), pwFilings);
						Collection ff = (Collection) ((Map) v).getOrDefault("files", Collections.EMPTY_LIST);
						for (Object fe : ff) {
							String subFileName = Dust.access(fe, MindAccess.Peek, null, "name");
							File fp = new File(f.getParentFile(), subFileName);
							FileReader rdrChild = new FileReader(fp);
							Map child = (Map) parser.parse(rdrChild);
							rdrChild.close();
							processFilings(companyInfo, child, pwFilings);
						}

						pwFilings.flush();
						pwFilings.close();
					}
				}
			} else if ( null != v ) {
				companyInfo.put(k, v);
			}
		}

		return companyInfo;
	}

	public void processFilings(Map companyInfo, Map filingInfo, PrintWriter w) throws Exception {

		String cik = (String) companyInfo.get("cik");

		long count = ((Collection) filingInfo.get(EdgarSubmissionAtt.accessionNumber.name())).size();

		long fc = (long) companyInfo.getOrDefault(EdgarHeadFields.__FilingCount.name(), 0L);
		companyInfo.put(EdgarHeadFields.__FilingCount.name(), fc + count);

		Set<String> ft = (Set<String>) companyInfo.get(EdgarHeadFields.__FormTypes.name());
		if ( null == ft ) {
			ft = new TreeSet<>();
			companyInfo.put(EdgarHeadFields.__FormTypes.name(), ft);
		}

		EnumMap<EdgarSubmissionAtt, List> attValues = new EnumMap<EdgarSubmissionAtt, List>(EdgarSubmissionAtt.class);
		for (EdgarSubmissionAtt esa : EdgarSubmissionAtt.values()) {
			attValues.put(esa, (List) filingInfo.get(esa.name()));
		}

		for (int i = 0; i < count; ++i) {
			StringBuilder sbLine = null;

			for (Map.Entry<EdgarSubmissionAtt, List> ae : attValues.entrySet()) {
				EdgarSubmissionAtt esa = ae.getKey();
				Object val = (esa == EdgarSubmissionAtt.CIK) ? cik : ae.getValue().get(i);

				if ( esa == EdgarSubmissionAtt.form ) {
					ft.add((String) val);
				}

				if ( esa.str ) {
					val = DustUtils.csvEscape((String) val, true);
				}
				sbLine = DustUtils.sbAppend(sbLine, "\t", true, val);
			}

			w.println(sbLine);
		}
	}

	public void factGen() throws Exception {
		pm = new DustUtils.ProcessMonitor("Fact gen", 100);

		ExecutorService pool = Executors.newFixedThreadPool(10);

		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File f) {
				if ( f.isFile() && f.getName().endsWith(EXT_JSON) ) {
					try {
						File fCsv = new File(DustUtils.cutPostfix(f.getCanonicalPath(), ".") + EXT_CSV);
						if ( !fCsv.isFile() ) {
							pool.execute(new Runnable() {
								@Override
								public void run() {
									try {
										genFactCsv(pm, f, fCsv);
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							});
						}
					} catch (Throwable e) {
						DustException.swallow(e);
					}
				}

//				return 10000 < pm.getCount();
				return false;
			}
		};

		File dir = fFactRoot;
//		dir = new File(fFactRoot, "00");
		DustUtilsFile.searchRecursive(dir, ff);

//		System.out.println(ff);

		pool.shutdown();

		System.out.println(pm);
	}

	public void genFactCsv(DustUtils.ProcessMonitor pm, File f, File fCsv) throws Exception {
		pm.step();

		JSONParser p = jp.get();

		Map head = (Map) p.parse(new FileReader(f));
		Map<String, Object> facts = (Map) head.getOrDefault("facts", Collections.EMPTY_MAP);

		try (PrintStream ps = new PrintStream(fCsv)) {

			ps.println(EDGAR_FACT_HEADER);
			Map<EdgarFactField, Object> factData = new TreeMap<>();
			factData.put(EdgarFactField.cik, head.get("cik"));

			for (Map.Entry<String, Object> te : facts.entrySet()) {
				String t = te.getKey();
				factData.put(EdgarFactField.taxonomy, t);
				Map<String, Object> concepts = (Map) ((Map.Entry) te).getValue();
				for (Map.Entry<String, Object> ce : concepts.entrySet()) {
					String ck = ce.getKey();
					factData.put(EdgarFactField.concept, ck);
					Object units = ((Map<String, Object>) ce.getValue()).getOrDefault("units", Collections.EMPTY_MAP);

					for (Object va : ((Map) units).entrySet()) {
						Object uk = ((Map.Entry) va).getKey();
						factData.put(EdgarFactField.unit, uk);

						for (Object v : (Collection) ((Map.Entry) va).getValue()) {
							Map fact = (Map) v;
							for (EdgarFactField eff : EDGAR_FACT_EXT) {
								factData.put(eff, fact.get(eff.name()));
							}
							if ( null == factData.get(EdgarFactField.start) ) {
								factData.put(EdgarFactField.instant, factData.remove(EdgarFactField.end));
							} else {
								factData.remove(EdgarFactField.instant);
							}

							StringBuilder sbLine = null;
							for (EdgarFactField eff : EdgarFactField.values()) {
								sbLine = DustUtils.sbAppend(sbLine, "\t", true, factData.get(eff));
							}
							ps.println(sbLine);
						}
					}
				}
			}

			ps.flush();
		}
	}

	public void delGen() throws Exception {

		pm = new DustUtils.ProcessMonitor("Fact stats", 100);

		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File f) {
				if ( f.isFile() && f.getName().endsWith(EXT_CSV) ) {
					pm.step();
					f.delete();
				}

				return false;
			}
		};

		File dir = fFactRoot;
//		dir = new File(fFactRoot, "00");
		DustUtilsFile.searchRecursive(dir, ff);

		System.out.println(ff);

		System.out.println(pm);
	}

	public void factStats() throws Exception {

		pm = new DustUtils.ProcessMonitor("Fact stats", 100);

//		PrintWriter pwHead = new PrintWriter(fSubmissionIndex);
//		pwHead.println(EDGAR_HEAD_HEADER);

		int cut = fFactRoot.getCanonicalPath().length();

		FileFilter ff = new FileFilter() {

			long submissionCount = 0;
			long docCount = 0;

			@Override
			public boolean accept(File f) {
				if ( f.isFile() && f.getName().endsWith(EXT_CSV) ) {
					try {
						if ( pm.step() ) {
							System.out.println("Submissions to check so far: " + submissionCount);
						}

						Set<String> reports = new TreeSet<>();

						try (BufferedReader brf = new BufferedReader(new FileReader(f))) {
							DustUtilsData.TableReader trf = null;

							for (String linef; (linef = brf.readLine()) != null;) {
								String[] rowf = linef.split("\t");
								if ( null == trf ) {
									trf = new DustUtilsData.TableReader(rowf);
								} else {
									String accn = trf.get(rowf, EdgarFactField.accn.name());
									reports.add((String) accn);
								}
							}
						}

						if ( !reports.isEmpty() ) {
							String id = DustUtils.cutPostfix(f.getCanonicalPath().substring(cut), ".");
							File fSubmissions = new File(fSubmissionRoot, id + EXT_CSV);

							if ( fSubmissions.isFile() ) {
								submissionCount += reports.size();

								try (BufferedReader brf = new BufferedReader(new FileReader(fSubmissions))) {

									DustUtilsData.TableReader trf = null;

									for (String linef; (linef = brf.readLine()) != null;) {
										String[] rowf = linef.split("\t");
										if ( null == trf ) {
											trf = new DustUtilsData.TableReader(rowf);
										} else {
											String accn = trf.get(rowf, EdgarSubmissionAtt.accessionNumber.name());
											if ( reports.remove(accn) ) {
												String primaryDoc = trf.get(rowf, EdgarSubmissionAtt.primaryDocument.name());
												if ( primaryDoc.isEmpty() ) {
													DustUtils.breakpoint("Primary doc not found");
												} else {
													++docCount;
												}

												if ( reports.isEmpty() ) {
													break;
												}
											}
										}
									}

									if ( !reports.isEmpty() ) {
										DustUtils.breakpoint("Referred submissions not found", reports, "in", id);
									}
								}
							} else {
								DustUtils.breakpoint("Submission file not found");
							}
						}

					} catch (Throwable e) {
						DustException.swallow(e);
					}
				}

				return false;
			}

			@Override
			public String toString() {
				return "Referred submission count: " + submissionCount + " found doc count: " + docCount;
			}
		};

		File dir = fFactRoot;
//		dir = new File(fFactRoot, "00");
		DustUtilsFile.searchRecursive(dir, ff);

		System.out.println(ff);

		System.out.println(pm);
	}

	public void loadSubmissions() throws Exception {
		File dir = new File(edgarRoot, "submissions");
//		File dir = new File(edgarRoot, "submissions/00");
		int cut = dir.getCanonicalPath().length();

		pm = new DustUtils.ProcessMonitor("Loading submission data", 1000);

		PrintWriter pwHead = new PrintWriter(fSubmissionIndex);
		pwHead.println(EDGAR_HEAD_HEADER);

		FileFilter ff = new FileFilter() {
			long submissionCount = 0;

			@Override
			public boolean accept(File f) {
				if ( f.isFile() ) {
					try {
						String shortName = f.getCanonicalPath().substring(cut + 1);
						if ( shortName.endsWith("json") && !shortName.contains("submissions") ) {
							pm.step();

							Map c = processHead(f);

							StringBuilder sbLine = null;
							for (EdgarHeadFields ehf : EdgarHeadFields.values()) {
								Object val = c.get(ehf.name());

								switch ( ehf ) {
								case name:
								case description:
									val = DustUtils.csvEscape((String) val, true);
									break;
								case formerNames:
									val = ((null == val) || ((Collection) val).isEmpty()) ? null : DustUtils.csvEscape(val.toString(), true);
									break;
								case tickers:
								case exchanges:
									val = ((null == val) || ((Collection) val).isEmpty()) ? null : val.toString();
									break;
								case __FilingCount:
									submissionCount += (long) val;
									break;
								default:
									break;
								}
								sbLine = DustUtils.sbAppend(sbLine, "\t", true, val);
							}

							pwHead.println(sbLine);

//							companies.put(c.get("cik"), c);
						}
					} catch (Exception e) {
						DustException.swallow(e);
					}
				}

				return false;
			}

			@Override
			public String toString() {
				return "Total submission count: " + submissionCount;
			}
		};

		DustUtilsFile.searchRecursive(dir, ff);

		pwHead.flush();
		pwHead.close();

		System.out.println(ff);

		System.out.println(pm);

//		JSONValue.writeJSONString(companies, pwHead);
	}

}
