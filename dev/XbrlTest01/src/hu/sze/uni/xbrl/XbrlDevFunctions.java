package hu.sze.uni.xbrl;

import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.parser.JSONParser;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsFactory;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDevFunctions implements XbrlConsts {

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

	public static void jsonValidation() throws Exception {
		Set<String> knownDims = new HashSet<>(Arrays.asList("concept", "entity", "unit", "period", "language"));

		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
		XbrlFilingManager filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);

		JSONParser parser = new JSONParser();
		XbrlUtilsCounter dc = new XbrlUtilsCounter(true);

		int count = 0;
		int jsonCount = 0;

		boolean verbose = false;

		Map<String, Map> reportData = filings.getReportData();

		for (Map.Entry<String, Map> e : reportData.entrySet()) {
			String id = e.getKey();
			Map repSrc = e.getValue();

//			if ( !id.contains("lei:21380031XTLI9X5MTY92:2021-06-30:xbrl.org:ESEF-DK-0") )
//			if ( !id.contains("21380031XTLI9X5MTY92") )
//		if ( !id.contains("xbrl.org:ESEF-DK") )
//			if ( !id.contains("743700X6KUJ0Z8GJIF03:2021-12-31") )
//			{
//				continue;
//			}

			if ( 0 == (++count % 100) ) {
				System.out.println("Count " + count);
			}

			File jsonFile = null;
			
			try {
				jsonFile = filings.getReport(repSrc, XbrlReportType.GenJson, true);
			} catch (Throwable err) {
				DustException.swallow(err, "accessing facts for", id);
			}

			if ( null == jsonFile ) {
				continue;
			}

			Object report = parser.parse(new FileReader(jsonFile));
			Map valFacts = (Map) XbrlUtils.access(report, AccessCmd.Peek, null, "facts");

			if ( null == valFacts ) {
				continue;
			}

			++jsonCount;

			DustUtilsFactory<String, Set> extInfo = new DustUtilsFactory.Simple<String, Set>(true, HashSet.class);
			for (Object f : valFacts.values()) {
				Map dim = XbrlUtils.access(f, AccessCmd.Peek, null, "dimensions");
				String concept = (String) dim.get("concept");

				extInfo.get(concept).add(f);

				Set ks = new HashSet<>(dim.keySet());
				ks.removeAll(knownDims);

				((Map) f).put("__ds", ks.size());
			}

			int toMatch = valFacts.size();

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

			for (String[] rf : filings.getFacts(id)) {
				String ci = tr.get(rf, "Taxonomy") + ":" + tr.get(rf, "Concept");
				Set vset = extInfo.peek(ci);

				String tt = tr.get(rf, "Type");
				boolean isText = "string".equals(tt) || "text".equals(tt);

				String val = null;
				String testVal = null;
				String ov = tr.get(rf, "OrigValue");
				String valOrig = (ov.startsWith("\"")) ? ov.substring(1, ov.length() - 1) : ov;

				String valErr = null;
				String valTime = null;
				String time = null;
				String valUnit = null;
				String unit = null;

				int tmBefore = toMatch;

				if ( null == vset ) {
//					dc.add("Error CONCEPT " + ci);
					dc.add("Error CONCEPT " + id);
					if ( verbose ) {
//						System.out.println(id + " CSV fact not found in json " + ci);
					}
				} else {
					ArrayList<Map> ctxMatch = new ArrayList<>();

					Map lastMatch = null;

					for (Object vv : vset) {
						Map tf = (Map) vv;
						Map dim = (Map) tf.get("dimensions");

						valUnit = (String) dim.getOrDefault("unit", "-");
						unit = tr.get(rf, "Unit");

						if ( !DustUtils.isEqual(unit, valUnit) ) {
							continue;
						}

						valTime = (String) dim.get("period");
						time = tr.get(rf, "Instant");
						if ( DustUtils.isEmpty(time) ) {
							time = dateToTime(tr.get(rf, "StartDate"), false) + "/" + dateToTime(tr.get(rf, "EndDate"), true);
						} else {
							time = dateToTime(time, true);
						}

						if ( !DustUtils.isEqual(time, valTime) ) {
							continue;
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

							val = tr.get(rf, "Value");
							testVal = (String) tf.get("value");

							if ( null == testVal ) {
							} else if ( "-0".equals(testVal) ) {
								testVal = "0";
							} else if ( testVal.contains(".") && testVal.endsWith("0") ) {
								testVal = testVal.replaceAll("(0+)$", "");
							}
							if ( null != val ) {
								if ( val.contains(".") && val.endsWith("0") ) {
									val = val.replaceAll("(0+)$", "");
								}
								if ( val.endsWith(".") ) {
									val = val.substring(0, val.length() - 1);
								}
							}

							if ( isText || DustUtils.isEqual(testVal, val) ) {
								lastMatch = tf;
							}
						}
					}

					String fmt = tr.get(rf, "Format");
					fmt = DustUtils.getPostfix(fmt, ":");

					if ( null != lastMatch ) {
						vset.remove(lastMatch);
						--toMatch;
						dc.add("Success " + fmt);
						dc.add("Success <ALL>");
					} else {
//						dc.add("Error VALUE " + id);
						dc.add("Error VALUE " + fmt);
						dc.add("Error VALUE <ALL>");

						String scale = tr.get(rf, "Scale");
						String dec = tr.get(rf, "Dec");
						String sign = tr.get(rf, "Sign");

						if ( verbose ) {
							if ( !DustUtils.isEmpty(testVal) && !DustUtils.isEmpty(val) ) {
								Dust.dump("\t", true, id, testVal, val, ov, fmt, scale, dec, sign, time, ci);
							} else if ( DustUtils.isEmpty(testVal) ) {
								Dust.dump("\t", true, id, "<null>", val, ov, fmt, scale, dec, sign, time, ci);
							} else {
								Dust.dump("\t", true, id, testVal, "<null>", ov, fmt, scale, dec, sign, time, ci);
							}
						}
					}

					if ( ctxMatch.isEmpty() ) {
						if ( verbose ) {
							Dust.dumpObs("  NO match found for", ci, "local", DustUtils.sbAppend(null, ", ", true, (Object[]) rf));
						}
						dc.add("Error CONTEXT " + id);
					} else if ( 1 < ctxMatch.size() && (null != val) && !val.startsWith("Txt len") ) {
						// multiple appearance of the same fact, quite usual in xhtml - but MUST be the
						// same value!

						Set<Object> seen = new HashSet<>();
						for (Map m : ctxMatch) {
							Object cv = m.get("value");
							if ( !DustUtils.isEqual(val, cv) ) {
								if ( seen.add(cv) ) {
//									Dust.dumpObs("  Report error: conflict with known value", val, "in", m);
								}
							}
						}
					}
				}

				int diff = tmBefore - toMatch;

				if ( 1 < diff ) {
					System.out.println("multimatch " + ci + " count " + diff + " data " + DustUtils.sbAppend(null, ", ", true, (Object[]) rf));
				}
			}
		}

		System.out.println("Json count " + jsonCount);

		dc.dump("Summary");
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
