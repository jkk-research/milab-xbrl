package hu.sze.uni.xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.utils.DustUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDevFunctions implements XbrlConsts {
	static final Object VAL_EMPTY = new Object();
	static final Object VAL_NOCHECK = new Object();

	public static class CurrencyConverter {
		public final Pattern ptUnitCurrency = Pattern.compile("ISO4217:(\\w+).*");

		String mainCurrency;
		Map<String, Map<String, Double>> rates = null;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		public CurrencyConverter(String mainCurrency, String rateFilePath, String sep) throws Exception {
			this.mainCurrency = mainCurrency;
			String[] header = null;

			try (BufferedReader br = new BufferedReader(new FileReader(rateFilePath))) {
				for (String line; (line = br.readLine()) != null;) {
					String[] row = line.split(sep);

					if ( null == rates ) {
						rates = new TreeMap<>();
						for (int i = 1; i < row.length; ++i) {
							rates.put(row[i], new TreeMap<>());
						}
						rates.put(mainCurrency, new TreeMap<>());
						header = row;
					} else {
						String date = row[0].replace(".", "-");

						for (int i = 1; i < row.length; ++i) {
							String val = row[i];
							if ( !DustUtils.isEmpty(val) && !"N/A".equals(val) ) {
								Double bd = new Double(val);
								rates.get(header[i]).put(date, bd);
							}
						}
					}
				}
			}
		}

		public String optGetUnitCurrency(Map fact) {
			String ret = null;

			String unit = (String) fact.get("Unit");

			unit = unit.toUpperCase().trim();
			Matcher m = ptUnitCurrency.matcher(unit);

			if ( m.matches() ) {
				ret = m.group(1);
			}

			return ret;
		}

		public Double optConvert(Map fact) throws Exception {
			String vv = (String) fact.get("Value");
			if ( DustUtils.isEmpty(vv) ) {
				return null;
			}

			String ccy = optGetUnitCurrency(fact);

			if ( DustUtils.isEmpty(ccy) ) {
				return null;
			}

			Double val = null;
			Double div = null;
			String status = null;
			int count = 0;

			Map<String, Double> dateRate = rates.get(ccy);
			if ( null == dateRate ) {
				status = "Unknown currency";
			} else {
				val = new Double(vv);

				if ( mainCurrency.equals(ccy) ) {
					status = "No conversion";
					div = 1.0;
				} else {
					String tInstant = (String) fact.get("Instant");
					String tStart = (String) fact.get("StartDate");
					String tEnd = (String) fact.get("EndDate");
					
					if ( !DustUtils.isEmpty(tStart) && DustUtils.isEqual(tStart, tEnd) ) {
						tInstant = tStart;
					}

					if ( !DustUtils.isEmpty(tInstant) ) {
						div = dateRate.get(tInstant);
						if ( null != div ) {
							count = 1;
							val = val / div;
							status = DustUtils.isEmpty(tStart) ? "SUCCESS" : "SUCCESS *";
						} else {
							String lastDate = null;
							for (String rd : dateRate.keySet()) {
								if ( 0 < rd.compareTo(tInstant) ) {
									break;
								}
								lastDate = rd;
							}
							
							if ( null != lastDate ) {
						    Date firstDate = sdf.parse(tInstant);
						    Date secondDate = sdf.parse(lastDate);

						    long diffInMillies = Math.abs(secondDate.getTime() - firstDate.getTime());
						    long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

								if ( diff <= 7 ) {
									div = dateRate.get(lastDate);
									count = 1;
									val = val / div;
									status = "SUCCESS *";
								}
							}

							if ( null == div ) {
								val = null;
								status = "No rate for Instant";
							}
						}
					} else {
						for (Map.Entry<String, Double> er : dateRate.entrySet()) {
							String rd = er.getKey();
							if ( 0 <= rd.compareTo(tStart) ) {
								if ( 0 < rd.compareTo(tEnd) ) {
									break;
								}
								++count;
								Double rv = er.getValue();
								div = (null == div) ? rv : div + rv;
							}
						}

						if ( 0 < count ) {
							div = div / count;
							val = val / div;
							status = "SUCCESS";
						} else {
							val = null;
							status = "No rate for Period";
						}
					}
				}
			}

			if ( null != val ) {
				String dec = (String) fact.get("Dec");
				int decInt = 0;
				if ( !DustUtils.isEmpty(dec) && !"INF".equals(dec) ) {
					decInt = Integer.parseInt(dec);
				}

				BigDecimal bd = new BigDecimal(val);

				if ( 0 != decInt ) {
					bd = bd.setScale(decInt, RoundingMode.FLOOR);
				}
				fact.put("cvtValue", bd.toPlainString());
				fact.put("cvtRate", div);
				fact.put("cvtCount", count);
			} else {
				val = Double.NaN;
			}

			fact.put("cvtStatus", status);
			fact.put("cvtCurrency", ccy);

			return val;
		}
	}

	public static void unitConvert() throws Exception {
		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
		XbrlFilingManager filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);

		Map<String, Map> reportData = filings.getReportData();

		CurrencyConverter cCvt = new CurrencyConverter("EUR", "work/excRate_5yr_v2.csv", ";");

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

				DustUtils.TableReader tr = filings.getTableReader(id);

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

		unitConvert();
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
