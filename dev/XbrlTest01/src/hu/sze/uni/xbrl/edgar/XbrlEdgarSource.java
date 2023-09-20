package hu.sze.uni.xbrl.edgar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.PrintWriter;
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
import hu.sze.uni.xbrl.XbrlUtilsCounter;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlEdgarSource implements XbrlEdgarConsts {

	final File edgarRoot;
	JSONParser parser = new JSONParser();

//	Map companies;

	XbrlUtilsCounter dc = new XbrlUtilsCounter(true);
	DustUtils.ProcessMonitor pm;
	private File fSubmissionIndex;

	public XbrlEdgarSource(File dataRoot) {
		edgarRoot = new File(dataRoot + "/sources/edgar");

		fSubmissionIndex = new File(edgarRoot, "SubmissionIndex.csv");
	}

	public static void main(String[] args) throws Exception {
		Dust.main(args);

		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");

		XbrlEdgarSource edgarSource = new XbrlEdgarSource(dataRoot);

//		edgarSource.loadSubmissions();
		edgarSource.processSubmissions("\"3711\".equals(sic)");

		edgarSource.dc.dump("Summary");
	}

	public void processSubmissions(String filter) throws Exception {
		pm = new DustUtils.ProcessMonitor("Find companies", 0);
		
		DustUtils.TableReader tr = null;
		Map<String, Object> values = new TreeMap<>();
		
		Object mvelFilter = DustUtils.isEmpty(filter) ? null : MVEL.compileExpression(filter);

		try (BufferedReader br = new BufferedReader(new FileReader(fSubmissionIndex))) {
			for (String line; (line = br.readLine()) != null;) {

				pm.step();
				
				String[] row = line.split("\t");
				if ( null == tr ) {
					tr = new DustUtils.TableReader(row);
					System.out.println(line);
				} else {
					values.clear();
					tr.get(row, values);
					for ( Map.Entry<String, Object> e : values.entrySet() ) {
						String sv = (String) e.getValue();
						
						if ( (null != sv) && sv.startsWith("[") ) {
							String[] vv = sv.substring(1, sv.length()-1).split(",");
							Set<String> s = new TreeSet<>();
							for ( String v : vv ) {
								s.add(v.trim());
							}
						}
					}
					
					if ( (null == mvelFilter ) || (Boolean)MVEL.executeExpression(mvelFilter, values) ) {
						System.out.println(line);
					}
				}
			}
		}
		
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
