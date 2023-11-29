package hu.sze.uni.xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.stream.DustStreamUrlCache;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsConsts.DustCloseableWalker;
import hu.sze.milab.dust.utils.DustUtilsData;
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

				DustCloseableWalker<String[]> facts = filings.getFacts(id);
				for (String[] rf : facts) {

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

		taxonomyTest(true);
	}

	public static void taxonomyTest(boolean dumpOnly) throws Exception {
		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");

		XbrlFilingManager filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);

		DustStreamUrlCache urlCache = new DustStreamUrlCache(new File(dataRoot, "urlCache"), false);
		File taxonomyRoot = new File(dataRoot, "taxonomies");
		File fRoot = new File(taxonomyRoot, "IFRSAT-2023-03-23");
		XbrlTaxonomyLoader taxonomyCollector = XbrlCoreUtils.readTaxonomy(urlCache, fRoot, "ifrs-full");
		taxonomyCollector.collectData();

		if ( dumpOnly ) {
//			taxonomyCollector.taxonomyBlocks("DisclosureOfGeographicalAreasLineItems");
			ArrayList<ArrayList<String>> ret = taxonomyCollector.taxonomyBlocks("Revenue");
			
			try (PrintWriter out = new PrintWriter("work/TaxOutRevenue.csv")) {
				for ( ArrayList<String> row : ret) {
					out.println(DustUtils.sbAppend(null, ",", true, row.toArray()));
				}
				out.flush();
			}
			
//			taxonomyCollector.taxonomyTree("Revenue");
//			taxonomyCollector.taxonomyTree("BenefitsPaidOrPayable");
			
//			taxonomyCollector.dump();
		} else {
			Map<String, Object> concepts = taxonomyCollector.peek(null, "item");

			ArrayList<String> ids = new ArrayList<>();
			File fTxt = new File("params/report_list_auto45.txt");
			if ( fTxt.isFile() ) {
				try (BufferedReader br = new BufferedReader(new FileReader(fTxt))) {
					for (String line; (line = br.readLine()) != null;) {
						String id = line.trim();
						if ( !DustUtils.isEmpty(id) ) {
							ids.add(id);
						}
					}
				}
			}

			try (PrintWriter psOut = new PrintWriter("work/test01.csv")) {
				XbrlUtils.exportConceptCoverage(psOut, filings, ids, concepts.keySet());
			}
		}
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
