package hu.sze.uni.xbrl;

import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.parser.JSONParser;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsFactory;
import hu.sze.milab.xbrl.XbrlConsts.XbrlFactDataType;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDevJsonValidation implements XbrlConsts {
	static final Object VAL_EMPTY = new Object();
	static final Object VAL_NOCHECK = new Object();

	public static void jsonValidation() throws Exception {
		Set<String> knownDims = new HashSet<>(Arrays.asList("concept", "entity", "unit", "period", "language"));

		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
		XbrlFilingManager filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);

		JSONParser parser = new JSONParser();
		XbrlUtilsCounter dc = new XbrlUtilsCounter(true);

		SimpleDateFormat dfmtIso = new SimpleDateFormat(FMT_DATE);

		int count = 0;
		int valCount = 0;
		int jsonCount = 0;
		
		int missCount = 0;

		boolean verbose = true;
		boolean doDelete = false;

		Map<String, Map> reportData = filings.getReportData();

		Set<Map> repToUpdate = new HashSet<>();

		Set<String> badInput = new HashSet<>(
				Arrays.asList("lei:969500E7V019H9NP7427:2022-12-31:xbrl.org:ESEF-FR-0", "lei:7437000I5X6LNQOW6U59:2020-12-31:xbrl.org:ESEF-FI-0", "lei:31570010000000102818:2020-12-31:xbrl.org:ESEF-CZ-0",
						"lei:529900D05TGFDEQACZ88:2022-12-31:xbrl.org:ESEF-HU-0", "lei:529900GPOO9ISPD1EE83:2020-12-31:xbrl.org:ESEF-AT-0", "lei:259400LGXW3K0GDAG361:2021-12-31:xbrl.org:ESEF-PL-0"));

		for (Map.Entry<String, Map> e : reportData.entrySet()) {
			if ( 0 == (++count % 100) ) {
				System.out.println("Count " + count);
			}

			String id = e.getKey();
			Map repSrc = e.getValue();

			if ( badInput.contains(id) ) {
				continue;
			}

//			if ( !id.contains("lei:21380031XTLI9X5MTY92:2021-06-30:xbrl.org:ESEF-DK-0") )
//			if ( !id.contains("21380031XTLI9X5MTY92") )
//			if ( !id.contains("xbrl.org:ESEF-DK") )
//			if ( id.contains("lei:74780000B0QHXQ0LQW20:2022-12-31:xbrl.org:ESEF-HR-0") ) {
//				continue;
//			}

			DustUtils.TableReader tr = filings.getTableReader(id);

			if ( null == tr ) {
				System.out.println("PROCESS, Accessing facts for report " + id);
				try {
					filings.getReport(repSrc, XbrlReportType.ContentVal, true);
					tr = filings.getTableReader(id);
				} catch (Throwable err) {
					DustException.swallow(err, "accessing facts for", id);
				}

				if ( null == tr ) {
					System.out.println("ERROR, facts not found for report " + id);
					continue;
				}
			}

			int extDimCount = (tr.getColIdx("OrigValue") - tr.getColIdx("Instant") - 1) / 2;

			++valCount;

			File jsonFile = null;

			try {
				jsonFile = filings.getReport(repSrc, XbrlReportType.GenJson, true);
			} catch (Throwable err) {
				DustException.swallow(err, "accessing facts for", id);
			}

			if ( null == jsonFile ) {
				continue;
			}

//			File ccc = filings.getReport(repSrc, XbrlReportType.ContentVal, false);
//			System.out.println(ccc.getCanonicalPath());

			Object report = parser.parse(new FileReader(jsonFile));
			Map valFacts = (Map) XbrlUtils.access(report, AccessCmd.Peek, null, "facts");

			if ( null == valFacts ) {
				continue;
			}

			++jsonCount;

			for (Object f : valFacts.values()) {
				Map dim = XbrlUtils.access(f, AccessCmd.Peek, null, "dimensions");

				Set ks = new HashSet<>(dim.keySet());
				ks.removeAll(knownDims);

				((Map) f).put("__ds", ks.size());
			}
			
			ArrayList<Map> jsonFacts = new ArrayList<>((Collection<Map>) valFacts.values());
			int fcJson = jsonFacts.size();
			int fcCsv = 0;

			ArrayList<Map> ctxMatch = new ArrayList<>();
			ArrayList<Integer> valMatch = new ArrayList<>();
			
			for (String[] rf : filings.getFacts(id)) {
				++fcCsv;
				String ci = tr.get(rf, "Taxonomy") + ":" + tr.get(rf, "Concept");

				String tt = tr.get(rf, "Type");
				XbrlFactDataType factType = XbrlFactDataType.valueOf(tt);

				String fmt = tr.get(rf, "Format");
				fmt = DustUtils.getPostfix(fmt, ":");

				String time = tr.get(rf, "Instant");
				if ( DustUtils.isEmpty(time) ) {
					time = dateToTime(tr.get(rf, "StartDate"), false) + "/" + dateToTime(tr.get(rf, "EndDate"), true);
				} else {
					time = dateToTime(time, true);
				}

				String unit = tr.get(rf, "Unit");
				String dec = tr.get(rf, "Dec");

				int decInt = 0;

				if ( !DustUtils.isEmpty(dec) && !"INF".equals(dec) ) {
					decInt = Integer.parseInt(dec);
				}

				String val = tr.get(rf, "Value");

				String valJson = null;
				String ov = tr.get(rf, "OrigValue");
//				String valOrig = (ov.startsWith("\"")) ? ov.substring(1, ov.length() - 1) : ov;
//				String err = tr.get(rf, "Err");

				Object objVal = null;
				Object objValJson = null;

				boolean conceptFound = false;
				ctxMatch.clear();
				valMatch.clear();

				for (int jsonIdx = jsonFacts.size(); jsonIdx-->0; ) {
					Map tf = jsonFacts.get(jsonIdx);
					Map dim = (Map) tf.get("dimensions");

					String concept = (String) dim.get("concept");
					if ( !DustUtils.isEqual(ci, concept) ) {
						continue;
					}

					conceptFound = true;

					String period = (String) dim.get("period");
					if ( !DustUtils.isEqual(time, period) ) {
						continue;
					}

					if ( "number".equals(tt) ) {
						String valUnit = (String) dim.getOrDefault("unit", "-");
						if ( "-".equals(valUnit) && "xbrli:pure".equals(unit) ) {
							valUnit = "xbrli:pure";
						}

						if ( !"fixed-zero".equals(fmt) && !DustUtils.isEqual(unit, valUnit) ) {
							continue;
						}
					}

					int dimsToMatch = (int) tf.get("__ds");
					for (int i = 1; i <= extDimCount; ++i) {
						String axis = tr.get(rf, "Axis_" + i);
						if ( !DustUtils.isEmpty(axis) ) {
							String d = tr.get(rf, "Dim_" + i);
							Object vd = dim.getOrDefault(axis, "");
							if ( DustUtils.isEqual(d, vd) ) {
								--dimsToMatch;
							} else {
								dimsToMatch = -1;
								break;
							}
						}
					}

					if ( 0 == dimsToMatch ) {
						ctxMatch.add(tf);

						try {
							if ( null == objVal ) {
								objVal = strToObj(factType, val, decInt, dfmtIso);
							}

							valJson = (String) tf.get("value");
							objValJson = null;

							if ( VAL_NOCHECK == objVal) {
								valMatch.add(jsonIdx);
							} else if (VAL_EMPTY == objVal) {
								if ( DustUtils.isEmpty(valJson)) {
									valMatch.add(jsonIdx);
								}
							} else if (DustUtils.isEmpty(valJson)) {
								if ( VAL_EMPTY == objVal) {
									valMatch.add(jsonIdx);
								}
							} else {
								objValJson = strToObj(factType, valJson, decInt, dfmtIso);

								if ( 0 == ((Comparable)objVal).compareTo(objValJson) ) {
									valMatch.add(jsonIdx);
								}
							}
						} catch (Throwable eee) {
							DustException.swallow(eee, "Value parse error");
						}
					}
				}

				if ( conceptFound ) {

					int ctxMatchCount = ctxMatch.size();
					if ( 0 < ctxMatchCount ) {

						int valMatchCount = valMatch.size();
						if ( 0 < valMatchCount ) {
							jsonFacts.remove((int) valMatch.get(0));
//							if ( !jsonFacts.remove(valMatch.get(0))) {
//								System.out.println("hopp");
//							}
//							--toMatch;
							dc.add("Success " + fmt);
							dc.add("Success <ALL>");
						} else {
							verbose = false;
							switch ( factType ) {
							case bool:
								break;
							case date:
								break;
							case empty:
								break;
							case number:
								verbose = true;
								break;
							case string:
							case text:
								break;
							}
							dc.add("Error VALUE fmt  " + fmt);
							dc.add("Error VALUE type " + tt);
							dc.add("Error VALUE <ALL>");
//					dc.add("Error VALUE id   " + id);

							if ( verbose ) {
								String scale = tr.get(rf, "Scale");
								String sign = tr.get(rf, "Sign");
								Dust.dump("\t", true, id, valJson, objValJson, objVal, ov, fmt, scale, dec, sign, time, ci);
							}
						}
					} else {
						if ( verbose ) {
//							Dust.dumpObs("  NO match found for", ci, "local", DustUtils.sbAppend(null, ", ", true, (Object[]) rf));
						}
//						dc.add("Error CONTEXT " + id);
						dc.add("Error CONTEXT <ALL>");
					}
				} else {
					dc.add("Error CONCEPT <ALL>");
//					dc.add("Error CONCEPT " + repSrc.get("country"));
					if ( verbose ) {
//			System.out.println(id + " CSV fact not found in json " + ci);
					}
				}
			}
			
			if ( fcJson != fcCsv ) {
				dc.add("Error JSON/CSV COUNT <ALL>");
			}
			
//			if (0 < toMatch) {
			if ( !jsonFacts.isEmpty() ) {
				missCount += jsonFacts.size();
				dc.add("Error JSON MISSED <ALL>");
			}
		}

		dc.dump("Summary");

		System.out.println("FILE COUNTS - all reports: " + count + ", parsed CSV: " + valCount + ", Json available: " + jsonCount);
		System.out.println("FACT COUNTS - missed in CSV: " + missCount);

		System.out.println("Selected for update [");
		for (

		Map toUpdate : repToUpdate) {
			System.out.println("  " + toUpdate.get("fxo_id"));

			if ( doDelete ) {
				File csv = filings.getReport(toUpdate, XbrlReportType.ContentVal, false);
				if ( null != csv ) {
					csv.delete();
					System.out.println("   deleted " + csv.getCanonicalPath());
				}
			}
		}
		System.out.println("] " + repToUpdate.size() + " reports.");
	}

	static Object strToObj(XbrlFactDataType factType, String val, int decInt, SimpleDateFormat dfmtIso) throws Exception {
		Object objVal = null;

		if ( DustUtils.isEmpty(val) ) {
			switch ( factType ) {
			case bool:
			case date:
			case number:
			case empty:
				objVal = VAL_EMPTY;
				break;
			case string:
			case text:
				objVal = VAL_NOCHECK;
				break;
			}
		} else {
			switch ( factType ) {
			case bool:
				objVal = Boolean.valueOf(val);
				break;
			case date:
				objVal = dfmtIso.parse(val);
				break;
			case number:
				BigDecimal bd = new BigDecimal(val);
				if ( 0 != decInt ) {
					bd = bd.setScale(decInt, RoundingMode.FLOOR);
				}
				objVal = bd;
				break;
			case empty:
				objVal = VAL_EMPTY;
				break;
			case string:
			case text:
				objVal = VAL_NOCHECK;
				break;
			}
		}
		return objVal;
	}

	static DustUtilsFactory<String, String> nextDay = new DustUtilsFactory<String, String>(true) {
		SimpleDateFormat fmtOut = new SimpleDateFormat("yyyy-MM-dd");

		@Override
		protected String create(String key, Object... hints) {
			try {
				Date dt = fmtOut.parse(key);
				Calendar c = Calendar.getInstance();
				c.setTime(dt);
				c.add(Calendar.DATE, 1);
				dt = c.getTime();
				return fmtOut.format(dt);
			} catch (ParseException e) {
				return DustException.wrap(e, "Date parse error from", key);
			}
		}
	};

	public static String dateToTime(String d, boolean end) {
		return (end ? nextDay.get(d) : d) + "T00:00:00";
	}

	public static void main(String[] args) throws Exception {
		Dust.main(args);

//		testDateConv();
		jsonValidation();
//		sum();
	}
}
