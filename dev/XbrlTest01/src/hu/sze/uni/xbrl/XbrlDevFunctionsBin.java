package hu.sze.uni.xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsData;
import hu.sze.milab.dust.utils.DustUtilsFactory;
import hu.sze.milab.xbrl.XbrlCoreUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDevFunctionsBin implements XbrlConsts {

	public static void testDateConv() throws Exception {
		File f = new File("work/AllDate_Filtered.csv");
		PrintWriter result = new PrintWriter("work/DatePostProc.csv");

		SimpleDateFormat fmtOut = new SimpleDateFormat("yyyy-MM-dd");

		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			DustUtilsData.TableReader tr = null;

			for (String line; (line = br.readLine()) != null;) {
				String[] data = line.split("\t");

				if ( null == tr ) {
					tr = new DustUtilsData.TableReader(data);
				} else {
					String fmt = tr.get(data, "Format");
					tr.set(data, "Err", "");

					if ( fmt.contains("date") ) {
						fmt = DustUtils.getPostfix(fmt, ":");
						String val = tr.get(data, "OrigValue");
						if ( !DustUtils.isEmpty(val) ) {
							String v = val.substring(1, val.length() - 1);

							try {
								Date d = XbrlCoreUtils.convertToDate(v, fmt);

								if ( null == d ) {
									if ( !DustUtils.isEmpty(v) ) {
										DustException.wrap(null, "Format mismatch", fmt, v);
									}
								} else {
									v = fmtOut.format(d);
									tr.set(data, "Value", "\"" + v + "\"");
								}
							} catch (Exception e) {
								tr.set(data, "Err", e.toString());
							}
						}
					}

					line = DustUtils.sbAppend(null, "\t", true, (Object[]) data).toString();
				}

				result.println(line);
			}
		}

		result.flush();
		result.close();
	}

	public static void sum() throws Exception {
		Set<String> knownDims = new HashSet<>(Arrays.asList("concept", "entity", "unit", "period", "language"));

		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
		XbrlFilingManager filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);

		XbrlUtilsCounter dc = new XbrlUtilsCounter(true);

		Map<String, Map> reportData = filings.getReportData();

		for (Map.Entry<String, Map> e : reportData.entrySet()) {
			String id = e.getKey();
			Map repSrc = e.getValue();
			
			String c = (String) repSrc.get("country");
			String n = (String) repSrc.get("__EntityName");

			dc.add(c +  "\t<ALL>");
			dc.add(c + "\t" + n);
		}
		
//		PrintStream ps = System.out;
		PrintStream ps1 = new PrintStream("Summary.csv");
//		System.setOut(ps1);
		
		for ( Map.Entry<Object, Long> me : dc) {
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

		boolean verbose = true;

		Map<String, Map> reportData = filings.getReportData();

		for (Map.Entry<String, Map> e : reportData.entrySet()) {
			String id = e.getKey();
			Map repSrc = e.getValue();

			if ( !id.contains("21380031XTLI9X5MTY92") ) {
				continue;
			}

			if ( 0 == (++count % 100) ) {
				System.out.println("Count " + count);
			}

			File jsonFile = filings.getReport(repSrc, XbrlReportType.GenJson, true);

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

			DustUtilsData.TableReader tr = filings.getTableReader(id);

			if ( null == tr ) {
				System.out.println("PROCESS, Accessing facts for report " + id);
				filings.getReport(repSrc, XbrlReportType.ContentVal, true);
				tr = filings.getTableReader(id);

				if ( null == tr ) {
					System.out.println("ERROR, facts not found for report " + id);
					continue;
				}
			}

			int extDimCount = (tr.getColIdx("OrigValue") - tr.getColIdx("Instant") - 1) / 2;

			for (String[] rf : filings.getFacts(id)) {
				String ci = tr.get(rf, "Taxonomy") + ":" + tr.get(rf, "Concept");
				Set vset = extInfo.peek(ci);

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
					dc.add("Error CONCEPT " + id);
					if ( verbose ) {
						System.out.println(id + " CSV fact not found in json " + ci);
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

							if ( DustUtils.isEqual(testVal, val) ) {
								lastMatch = tf;
							}
						}
					}

					String fmt = tr.get(rf, "Format");

					if ( null != lastMatch ) {
						vset.remove(lastMatch);
						--toMatch;
						dc.add("Success Orig " + fmt);
					} else {
						dc.add("Error VALUE " + id);

						String scale = tr.get(rf, "Scale");
						String dec = tr.get(rf, "Dec");
						String sign = tr.get(rf, "Sign");

						if ( !DustUtils.isEmpty(testVal) && !DustUtils.isEmpty(val) ) {
							Dust.dump("\t", true, id, testVal, val, ov, fmt, scale, dec, sign, time, ci);
						} else if ( verbose ) {
							if ( DustUtils.isEmpty(testVal) ) {
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
					} else if ( 1 < ctxMatch.size() && !val.startsWith("Txt len") ) {
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

	public static void jsonValidation2() throws Exception {
		File dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
		XbrlFilingManager filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);

		DecimalFormat df = new DecimalFormat("#");
		df.setMaximumFractionDigits(8);
		SimpleDateFormat fmtOut = new SimpleDateFormat("yyyy-MM-dd");

		boolean downloadMode = false;
//		filings.downloadLimit = 2000;
		boolean clearMode = false;

		Map<String, Map> reportData = filings.getReportData();
		int count = 0;

		int jsonCount = 0;
		JSONParser parser = new JSONParser();

		XbrlUtilsCounter dc = new XbrlUtilsCounter(true);

		Set<String> knownDims = new HashSet<>(Arrays.asList("concept", "entity", "unit", "period", "language"));

		System.out.println("\nJSON validation of " + reportData.size() + " reports\n");

		List<String> filter = null;
		String[] genFiles = { "extractedJson.json", "Report_Val.csv", "Report_Txt.csv" };

//		filter = Arrays.asList("21380031XTLI9X5MTY92");

		for (Map.Entry<String, Map> e : reportData.entrySet()) {
			String id = e.getKey();
			Map repSrc = e.getValue();
			String repDirName = XbrlUtils.access(repSrc, AccessCmd.Peek, null, XbrlFilingManager.LOCAL_DIR);
			File repDir = new File(filings.getFilingRoot(), repDirName);

			if ( !id.contains("21380031XTLI9X5MTY92") ) {
////			if ( (null != filter) && !filter.contains(id) ) {
////				System.out.println("Deleting " + repDir);
////				repDir.delete();
				continue;
			}

//			if ( id.contains("xbrl.org:ESEF-DK") ) {
//				continue;
//			}

			if ( 0 == (++count % 100) ) {
				System.out.println("Count " + count);
			}

			if ( clearMode ) {
				for (String gf : genFiles) {
					File f = new File(repDir, gf);
					if ( f.isFile() ) {
						f.delete();
						dc.add(gf);
					}
				}
				continue;
			}

			File jsonFile = new File(repDir, "extractedJson.json");
			boolean jsonFileExists = jsonFile.isFile();

//			if ( jsonFileExists ) {
//				jsonFile.delete();
//				continue;
//			} else if ( !jsonFile.exists() ) {
//				continue;
//			}

			Map valFacts = null;

			if ( !jsonFileExists ) {

				File fJson;
				try {
					fJson = filings.getReport(repSrc, XbrlReportType.Json, downloadMode);
				} catch (Throwable t) {
					System.out.println("Download error " + id + " from: " + repSrc.get("json_url"));
					fJson = null;
				}

				if ( (null != fJson) && fJson.isFile() ) {
					Object report = parser.parse(new FileReader(fJson));

					valFacts = (Map) XbrlUtils.access(report, AccessCmd.Peek, null, "facts");
					for (Object f : valFacts.values()) {
						String val = XbrlUtils.access(f, AccessCmd.Peek, "", "value");
						if ( val.length() > TEXT_CUT_AT ) {
							val = val.substring(0, TEXT_CUT_AT);
							val = val.replaceAll("\\s+", " ");
							XbrlUtils.access(f, AccessCmd.Set, val, "value");
						}
					}

					FileWriter fw = new FileWriter(jsonFile);
					JSONValue.writeJSONString(report, fw);
					fw.flush();
					fw.close();
				}
			} else {
				Object report = parser.parse(new FileReader(jsonFile));
				valFacts = (Map) XbrlUtils.access(report, AccessCmd.Peek, null, "facts");
			}

			if ( null != valFacts ) {
				++jsonCount;

				if ( downloadMode ) {
					continue;
				}

				DustUtilsFactory<String, Set> extInfo = new DustUtilsFactory.Simple<String, Set>(true, HashSet.class);
				for (Object f : valFacts.values()) {
//					String concept = XbrlUtils.access(f, AccessCmd.Peek, null, "dimensions", "concept");
//					extInfo.get(concept).add(f);

					Map dim = XbrlUtils.access(f, AccessCmd.Peek, null, "dimensions");
					String concept = (String) dim.get("concept");
					extInfo.get(concept).add(f);

					Set ks = new HashSet<>(dim.keySet());
					ks.removeAll(knownDims);

					((Map) f).put("__ds", ks.size());
				}

				int toMatch = valFacts.size();
				int mm = 0;
				int lineCount = 0;
//				System.out.println("Validating " + id + " factCount " + toMatch + " from " + jsonFile.getCanonicalPath());

				DustUtilsData.TableReader tr = filings.getTableReader(id);

				if ( null == tr ) {
					System.out.println("PROCESS, Accessing facts for report " + id);
					filings.getReport(repSrc, XbrlReportType.ContentVal, true);
					tr = filings.getTableReader(id);

					if ( null == tr ) {
						System.out.println("ERROR, facts not found for report " + id);
						continue;
					}
				}

				int extDimCount = (tr.getColIdx("OrigValue") - tr.getColIdx("Instant") - 1) / 2;

				for (String[] rf : filings.getFacts(id)) {
					++lineCount;

					String ci = tr.get(rf, "Taxonomy") + ":" + tr.get(rf, "Concept");
					Set vset = extInfo.peek(ci);

					String val = null;
					String testVal = null;
					String ov = tr.get(rf, "OrigValue");
					String valOrig = (ov.startsWith("\"")) ? ov.substring(1, ov.length() - 1) : ov;

					String scale = tr.get(rf, "Scale");
					String dec = tr.get(rf, "Dec");
					String sign = tr.get(rf, "Sign");

					String fmt = tr.get(rf, "Format");
					if ( DustUtils.isEmpty(fmt) ) {
						fmt = "ixt4:num-dot-decimal";
					}
					String fmtCode = "";
					if ( !DustUtils.isEmpty(fmt) ) {
						int sep = fmt.indexOf(":");
						fmtCode = fmt.substring(sep + 1);
					}

					String valErr = null;
					String valTime = null;
					String time = null;
					String valUnit = null;
					String unit = null;

					int tmBefore = toMatch;

					if ( null == vset ) {
						if ( !downloadMode ) {
							dc.add("Error CONCEPT " + id);
							if ( null != filter )
								System.out.println(id + " CSV fact not found in json " + ci);
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
								if ( null == val ) {
									val = "0";
								}

								if ( val.startsWith("Txt len") ) {
									lastMatch = tf;
								} else {
									if ( "-0".equals(val) ) {
										val = "0";
									}

									testVal = (String) tf.get("value");

									if ( null == testVal ) {
										testVal = "0";
									} else if ( "-0".equals(testVal) ) {
										testVal = "0";
									} else if ( testVal.contains(".") && testVal.endsWith("0") ) {
										testVal = testVal.replaceAll("(0+)$", "");
									}

									if ( DustUtils.isEqual(testVal, val) ) {
										lastMatch = tf;
									}
//									if ( !DustUtils.isEqual(testVal, val) ) {
//										Dust.dumpObs("  Test mismatch", ci, "local", val, "json", testVal);
//									} else {
//										lastMatch = tf;
//										--toMatch;
//									}
								}
							}
						}

						if ( 1 < ctxMatch.size() ) {
							mm += (ctxMatch.size() - 1);
						}

						if ( null != lastMatch ) {
							vset.remove(lastMatch);
							--toMatch;
							dc.add("Success Orig " + fmtCode);
						} else {

							try {
								if ( fmtCode.startsWith("num") ) {
									BigDecimal dVal = XbrlCoreUtils.convertToNumber(valOrig, fmt, scale, dec, sign);
									val = dVal.toPlainString();
									if ( val.contains(".") && val.endsWith("0") ) {
										val = val.replaceAll("(\\.?0+)$", "");
									}
								} else if ( fmtCode.startsWith("date") ) {
									if ( DustUtils.isEmpty(val) ) {
										Date d = XbrlCoreUtils.convertToDate(valOrig, fmtCode);
										val = fmtOut.format(d);
									}
								} else if ( fmtCode.equals("fixed-empty") ) {
									val = "";
								}
							} catch (Throwable t) {
								valErr = t.toString();
							}

							if ( !DustUtils.isEqual(val, testVal) ) {
//								Dust.dumpObs("  Test mismatch", ci, "time", time, "json ", testVal, "csv line:", DustUtils.sbAppend(null, "|", true, (Object[]) rf));
								dc.add("Error VALUE " + id);

								if ( !DustUtils.isEmpty(testVal) && !DustUtils.isEmpty(val) ) {
									Dust.dump("\t", true, id, testVal, val, ov, fmt, scale, dec, sign, time, ci);
								} else if ( null != filter ) {
									if ( DustUtils.isEmpty(testVal) ) {
										Dust.dump("\t", true, id, "<null>", val, ov, fmt, scale, dec, sign, time, ci);
									} else {
										Dust.dump("\t", true, id, testVal, "<null>", ov, fmt, scale, dec, sign, time, ci);
									}
								}
							} else {
								dc.add("Success New " + fmtCode);
								vset.remove(lastMatch);
								--toMatch;
							}
						}

						if ( ctxMatch.isEmpty() ) {
							if ( null != filter )
								Dust.dumpObs("  NO match found for", ci, "local", DustUtils.sbAppend(null, ", ", true, (Object[]) rf));
							dc.add("Error CONTEXT " + id);
						} else if ( 1 < ctxMatch.size() && !val.startsWith("Txt len") ) {
							// multiple appearance of the same fact, quite usual in xhtml - but MUST be the
							// same value!

							Set<Object> seen = new HashSet<>();
							for (Map m : ctxMatch) {
								Object cv = m.get("value");
								if ( !DustUtils.isEqual(val, cv) ) {
									if ( seen.add(cv) ) {
//										Dust.dumpObs("  Report error: conflict with known value", val, "in", m);
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

//				if ( 0 != toMatch ) {
//					System.out.println("In " + id + " JSON - CSV diff: " + (valFacts.size() - lineCount) + " Not matched: " + toMatch + " multimatch " + mm + " from " + jsonFile.getCanonicalPath());
//					System.out.println();
//				}
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
//		jsonValidation();
		sum();
	}
}
