package hu.sze.uni.xbrl.edgar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.simple.parser.JSONParser;
import org.mvel2.MVEL;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsFile;
import hu.sze.uni.xbrl.XbrlUtils;
import hu.sze.uni.xbrl.XbrlUtilsCounter;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlEdgarSource implements XbrlEdgarConsts {

	final File edgarRoot;
	JSONParser parser = new JSONParser();

//	Map companies;

	XbrlUtilsCounter dc = new XbrlUtilsCounter(true);
	DustUtils.ProcessMonitor pm;
	private File fSubmissionIndex;
	private File fSubmissionRoot;
	private File fReportRoot;

	private static Long tsLastDownload = 0L;

	public XbrlEdgarSource(File dataRoot) {
		edgarRoot = new File(dataRoot + "/sources/edgar");

		fSubmissionRoot = new File(edgarRoot, "submissions");
		fReportRoot = new File(edgarRoot, "reports");
		fSubmissionIndex = new File(edgarRoot, "SubmissionIndex.csv");
	}

	private static synchronized void safeDownload(String url, File file) throws Exception {
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
		edgarSource.processSubmissions("\"3711\".equals(company.sic)", "filing.form.contains(\"10-Q\")", "company.name.contains(\"Tesla\") && filing.reportDate.startsWith(\"202\")");
//		edgarSource.addPath();

		edgarSource.dc.dump("Summary");
	}

	public File getFiling(Map company, Map filing) throws Exception {
		File f = null;

		String formType = (String) filing.get(EdgarSubmissionAtt.form.name());
		String accn = (String) filing.get(EdgarSubmissionAtt.accessionNumber.name());

		File dir = new File(fReportRoot, company.get(EdgarHeadFields.__PathPrefix.name()) + "/" + formType + "/" + accn);
		String docName = (String) filing.get(EdgarSubmissionAtt.primaryDocument.name());

		boolean docNameMissing = DustUtils.isEmpty(docName);
		if ( docNameMissing ) {
			docName = "index.json";
		}

		dir.mkdirs();
		f = new File(dir, docName);

		if ( !f.isFile() ) {
			String url = EDGAR_URL_DATA + company.get(EdgarHeadFields.cik.name()) + "/" + accn.replace("-", "") + "/" + docName;
			safeDownload(url, f);

			if ( docNameMissing ) {
				// TODO post process the json, get the zip, extract, return the main file
			}
		}

		return f;
	}

	public void processSubmissions(String filterHead, String filterFiling, String filterDownload) throws Exception {
		
		pm = new DustUtils.ProcessMonitor("Find companies", 0);

		DustUtils.TableReader tr = null;
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
					tr = new DustUtils.TableReader(row);
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

								DustUtils.TableReader trf = null;

								for (String linef; (linef = brf.readLine()) != null;) {
									String[] rowf = linef.split("\t");
									if ( null == trf ) {
										trf = new DustUtils.TableReader(rowf);
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
