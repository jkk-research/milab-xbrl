package hu.sze.uni.xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.stream.DustStreamUrlCache;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsData;
import hu.sze.milab.dust.utils.DustUtilsFactory;
import hu.sze.milab.xbrl.XbrlCoreUtils;
import hu.sze.milab.xbrl.test.XbrlTaxonomyLoader;
import hu.sze.milab.xbrl.tools.XbrlToolsCurrencyConverter;

@SuppressWarnings({ "rawtypes" })
public class XbrlDevFunctions implements XbrlConsts {
	static final Object VAL_EMPTY = new Object();
	static final Object VAL_NOCHECK = new Object();

	public static void unitConvert() throws Exception {
		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
		XbrlFilingManager filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);

		Map<String, Map> reportData = filings.getReportData();

		XbrlToolsCurrencyConverter cCvt = new XbrlToolsCurrencyConverter("EUR", "params/excRate_5yr_v2.csv", ";");

		String[] cols = { "StartDate", "EndDate", "Instant", "Value", "Unit", "cvtValue", "cvtCurrency", "cvtStatus", "cvtRate", "cvtCount" };
//		String[] cols = {"Report", "Entity", "Taxonomy", "Concept", "StartDate", "EndDate", "Instant", "Value", "Unit"};

		XbrlUtilsCounter dc = new XbrlUtilsCounter(true);
		int cvtCount = 0;

		try (PrintWriter out = new PrintWriter("work/ConvTest.csv")) {
			StringBuilder sb = DustUtils.sbAppend(null, "\t", true, (Object[]) cols);
			out.println(sb);
			out.flush();

			Map fact = new HashMap();

			for (Map.Entry<String, Map> e : reportData.entrySet()) {
				String id = e.getKey();

				DustUtilsData.TableReader tr = filings.getTableReader(id);

				if ( null == tr ) {
					continue;
				}

				for (String[] rf : filings.getFacts(id)) {

					fact.clear();

					tr.getUntil(rf, fact, null);

					Double cVal = cCvt.optConvert(fact);

					if ( null != cVal ) {
						sb = null;
						for (String c : cols) {
							sb = DustUtils.sbAppend(sb, "\t", true, fact.get(c));
						}
						out.println(sb);
						out.flush();

						++cvtCount;

						if ( 0 == cvtCount % 50000 ) {
							System.out.println("  Process " + cvtCount);
						}

						String stat = (String) fact.get("cvtStatus");
						String ccy = (String) fact.get("cvtCurrency");

						switch ( stat ) {
						case "No conversion":
						case "SUCCESS":
						case "SUCCESS *":
							stat = ccy + ", " + stat + ", , ,";
							break;
						default:
							stat = DustUtils.sbAppend(null, ", ", true, ccy, stat, fact.get("StartDate"), fact.get("EndDate"), fact.get("Instant")).toString();
							break;
						}

						dc.add(stat);
					}

//				String ccy = cCvt.optGetUnitCurrency(tr, rf);
//				dc.add("Currency: " + ccy);
//
//				dc.add("Orig: " + tr.get(rf, "Unit"));
				}
			}
		}

		dc.dump("Summary");

		System.out.print("Conversion count: " + cvtCount);
	}

	public static void main(String[] args) throws Exception {
		Dust.main(args);

		taxonomyTest();
	}

	public static void taxonomyTest() throws Exception {
		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");

		File fTxt = new File("params/report_list_auto45.txt");

		DustDevCounter countConcepts = new DustDevCounter(true);
		DustUtilsFactory<String, DustDevCounter> data = new DustUtilsFactory<String, DustDevCounter>(true) {
			@Override
			protected DustDevCounter create(String key, Object... hints) {
				return new DustDevCounter(true);
			}
		};
		DustUtilsData.Indexer<String> expCols = new DustUtilsData.Indexer<>();
		int row = 0;

		if ( fTxt.isFile() ) {
			XbrlFilingManager filings = new XbrlFilingManager(dataRoot, true);
			filings.setDownloadOnly(false);

			DustUtilsData.TableReader tr = null;
			Iterable<String[]> repFacts = null;

			try (BufferedReader br = new BufferedReader(new FileReader(fTxt))) {
				for (String line; (line = br.readLine()) != null;) {
					String id = line.trim();
					++row;

					tr = filings.getTableReader(id);
					repFacts = filings.getFacts(id);

					int diStart = tr.getColIdx("Instant");
					int diEnd = tr.getColIdx("OrigValue");

					Set<String> axes = new TreeSet<>();
					Set<String> cols = new TreeSet<>();

					for (String[] rf : repFacts) {
						String taxonomy = tr.get(rf, "Taxonomy");
						if ( !DustUtils.isEqual("ifrs-full", taxonomy) ) {
							continue;
						}

						String concept = tr.get(rf, "Concept");

						String time = tr.get(rf, "Instant");
						if ( DustUtils.isEmpty(time) ) {
							time = tr.format(rf, " / ", "StartDate", "EndDate");
						}

						axes.clear();
						for (int i = diStart + 1; i < diEnd; i += 2) {
							String aVal = rf[i];
							if ( !DustUtils.isEmpty(aVal) ) {
								axes.add(aVal);
							}
						}

						String axisId = axes.isEmpty() ? "" : (" " + axes.toString());
						countConcepts.add(concept + axisId);
						
						String cellId = "(" + time + ")" + axisId;
						cols.add(cellId);

						data.get(concept).add(row + " " + cellId);
					}

					for (String cellId : cols) {
						expCols.getIndex(row + " " + cellId);
					}
				}
			}
		}

		countConcepts.dump("Concept stats");

		Dust.dumpObs("Collected data");
		for (String k : data.keys()) {
			DustDevCounter dc = data.peek(k);

			Dust.dumpObs(k);
			for (Map.Entry<Object, Long> e : dc) {
				String cellId = (String) e.getKey();
				int colIdx = expCols.peekIndex(cellId);

				Dust.dumpObs("  ", cellId, colIdx, e.getValue());
			}
		}

		if ( 0 < row ) {
			return;
		}

		DustStreamUrlCache urlCache = new DustStreamUrlCache(new File(dataRoot, "urlCache"), false);
		File taxonomyRoot = new File(dataRoot, "taxonomies");
		File fRoot = new File(taxonomyRoot, "IFRSAT-2023-03-23");
		XbrlTaxonomyLoader taxonomyCollector = XbrlCoreUtils.readTaxonomy(urlCache, fRoot);
		taxonomyCollector.collectData();
//		Map<String, Object> concepts = taxonomyCollector.peek(null, "item");

	}

	public static void sum() throws Exception {
		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
		XbrlFilingManager filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);

		XbrlUtilsCounter dc = new XbrlUtilsCounter(true);

		Map<String, Map> reportData = filings.getReportData();

		for (Map.Entry<String, Map> e : reportData.entrySet()) {
//			String id = e.getKey();
			Map repSrc = e.getValue();

			String c = (String) repSrc.get("country");
			String n = (String) repSrc.get("__EntityName");

			dc.add(c + "\t<ALL>");
			dc.add(c + "\t" + n);
		}

//		PrintStream ps = System.out;
		PrintStream ps1 = new PrintStream("Summary.csv");
//		System.setOut(ps1);

		for (Map.Entry<Object, Long> me : dc) {
			ps1.println(me.getKey() + "\t" + me.getValue());
		}

//		dc.dump("Summary");

		ps1.flush();
		ps1.close();

//		System.setOut(ps);
	}
}
