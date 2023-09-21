package hu.sze.uni.xbrl.edgar;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsFile;
import hu.sze.uni.xbrl.XbrlConsts;
import hu.sze.uni.xbrl.XbrlUtilsCounter;
import hu.sze.uni.xbrl.portal.XbrlTestPortalUtils;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlEdgarTest implements XbrlConsts {

	private static File EDGAR_ROOT = new File("/Users/lkedves/work/xbrl/data/sources/edgar");
	private static JSONParser parser = new JSONParser();

	private static XbrlUtilsCounter dc = new XbrlUtilsCounter(true);

	static Map index;

	private static int count = 0;

	static Pattern PT_FID = Pattern.compile("CIK(\\w+).*");

	static Set<String> ids = new TreeSet<>();

	public static void main(String[] args) throws Exception {
		Dust.main(args);

		long t = System.currentTimeMillis();

		checkSubmissions();
//		fixFileLocation();
//		unzip("submissions", Integer.MAX_VALUE, null);
//		unzip("submissions", 0, "test");
//		unzip("companyfacts", false);
//		process("companyfacts");

//		File f = new File("work/EdgarTestExport.csv");
//		load(f, "us-gaap:AccountsPayableOther", "ifrs-full:AccountingProfit");

//		sum.close();

		dc.dump("Summary");

		System.out.println("Count: " + count + ", Time " + (System.currentTimeMillis() - t) + " msec.");

	}

	public static void addPath() throws Exception {
	}

	public static void checkSubmissions() throws Exception {
		File dir = new File(EDGAR_ROOT, "submissions");
//		File dir = new File(EDGAR_ROOT, "submissions/00");
		int cut = dir.getCanonicalPath().length();

		FileFilter ff = new FileFilter() {
			long fCount;
			long subCount;
			long fSize;

			@Override
			public String toString() {
				return "Total JSON count " + fCount + ", sub count: " + subCount + ", total size: " + fSize;
			}

			@Override
			public boolean accept(File f) {
				if ( f.isFile() ) {
					logCount();

					try {
						String shortName = f.getCanonicalPath().substring(cut + 1);
//						String[] path = shortName.split("/");

						if ( shortName.endsWith("json") ) {
//							if ( path[0].startsWith("0") && path[1].endsWith("json") ) {
//						if ( "00".equals(path[0]) && path[1].startsWith("0") && path[2].endsWith("json") ) {
							++fCount;
							fSize += f.length();

							Map subInfo = (Map) parser.parse(new FileReader(f));

							boolean subFile = shortName.contains("submissions");
							if ( subFile ) {
								++subCount;
								String[] sp = f.getName().split("-");
								File fp = new File(f.getParentFile(), sp[0] + ".json");
								if ( !fp.isFile() ) {
									dc.add("Validation - Misplaced child file\t" + shortName + "\t");
								}
							} else {

								for (Object he : subInfo.entrySet()) {
									String k = (String) ((Map.Entry) he).getKey();
									Object v = ((Map.Entry) he).getValue();

									if ( null != v ) {
										dc.add("Key Head\t" + k + "\t" + v.getClass().getSimpleName());

										switch ( k ) {
										case "filings":
											for (Object fk : ((Map) v).keySet()) {
												dc.add("Key Filings\t" + fk + "\t");
											}

											break;
										case "exchanges":
											for (Object fk : (Collection) v ) {
												dc.add("Exchange\t" + fk + "\t");
											}
											break;
										}
									}
								}

								Collection ff = Dust.access(subInfo, MindAccess.Peek, Collections.EMPTY_LIST, "filings", "files");
								for (Object fe : ff) {
									String subFileName = Dust.access(fe, MindAccess.Peek, null, "name");
									File fp = new File(f.getParentFile(), subFileName);
									if ( !fp.isFile() ) {
										dc.add("Validation - Missing child file\t" + shortName + "\t" + subFileName);
									}
								}
							}

							Map fm = subFile ? subInfo : Dust.access(subInfo, MindAccess.Peek, Collections.EMPTY_MAP, "filings", "recent");
							Integer fs = null;

							for (Object fe : fm.entrySet()) {
								Object fk = ((Map.Entry) fe).getKey();

								dc.add("Key Submission\t" + fk + "\t");

								Collection fv = (Collection) ((Map.Entry) fe).getValue();
								int fc = fv.size();
								if ( null == fs ) {
									fs = fc;
								} else if ( fs != fc ) {
									dc.add("Validation - Submission arr length\t" + shortName + "\t" + fk);
								}
								
								if ( "form".equals(fk)) {
									for (Object ft : fv ) {
										dc.add("Form type\t" + ft + "\t");
									}
								}
							}
						}
					} catch (Exception e) {
						DustException.swallow(e);
					}
				}

				return false;
			}
		};

		DustUtilsFile.searchRecursive(dir, ff);

		System.out.println(ff);
	}

	public static void fixFileLocation() throws Exception {
		File root = new File(EDGAR_ROOT, "submissions");
		File dir = root;
//		File dir = new File(root, "00");
		int cut = dir.getCanonicalPath().length();

		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File f) {
				if ( f.isFile() ) {
					logCount();

					try {
						String shortName = f.getCanonicalPath().substring(cut + 1);

						if ( shortName.endsWith("json") && shortName.contains("submissions") ) {
							String[] path = f.getName().split("-");
							String id = path[0];
							String realHash = DustUtilsFile.getHashForID(id);
							String actualHash = DustUtils.cutPostfix(shortName, "/");
//							String actualHash = "00/" + DustUtils.cutPostfix(shortName, "/");

							if ( !DustUtils.isEqual(realHash, actualHash) ) {
								dc.add("Misplaced old file\t" + shortName + "\t" + realHash);

//								File fd = new File(root, realHash);
//								File fp = new File(fd, id + ".json");
//								if ( fp.isFile() ) {
//									dc.add("Planned move\t" + shortName + "\t" + realHash);
//									File ft = new File(fd, f.getName());
//									f.renameTo(ft);
//								} else {
//									System.out.println("hmm");
//								}
							}
						}
					} catch (Exception e) {
						DustException.swallow(e);
					}
				}
				return false;
			}
		};

		DustUtilsFile.searchRecursive(dir, ff);

		System.out.println(ff);

	}

	public static void logCount() {
		if ( 0 == (++count % 100) ) {
			System.out.println("Count " + count);
		}
	}

	static void unzip(String kind, int unzipCount, String subdir) throws Exception {

		File zipFile = new File(EDGAR_ROOT, kind + ".zip");
		File dir = new File(EDGAR_ROOT, kind);

//		File zipFile = new File("/Users/lkedves/work/xbrl/data/sources/edgar/submissions.zip");

		long compSize = 0;
		long totSize = 0;
		int count = 0;

		try (ZipFile zf = new ZipFile(zipFile)) {

			for (Enumeration<ZipArchiveEntry> ee = zf.getEntries(); ee.hasMoreElements();) {
				ZipArchiveEntry ze = ee.nextElement();

				boolean unzip = ++count < unzipCount;
				String fName = ze.getName();
				long realSize = ze.getSize();

				if ( unzip && !ze.isDirectory() ) {
					String id = DustUtils.cutPostfix(fName, ".");
					File d = new File(dir, (null == subdir) ? DustUtilsFile.getHashForID(id) : subdir);
					File f = new File(d, fName);

					if ( !f.exists() ) {
						XbrlTestPortalUtils.unzipEntry(zf, ze, f);
					} else {
						if ( realSize != f.length() ) {
							System.out.println("hmm");
						}
					}
				}

				dc.add(ze.isDirectory() ? "Dir" : "File");

//				if ( fName.contains("1318605")) {
//					dc.add(fName);
//				}

				Matcher m = PT_FID.matcher(fName);
				if ( m.matches() ) {
					ids.add(m.group(1));
//				} else {
//					dc.add("CIK NO " + fName);
				}

//				dc.add("CIK " + fName.startsWith("CIK") );

				logCount();

				compSize += ze.getCompressedSize();
				totSize += realSize;
//				zf.getInputStream(ze);

//			if ( (857400 < count) && (count < 857500) ){
//			System.out.println(fName + ": " + ze.getCompressedSize()  + ": " + ze.getSize() );
//		}

			}
		}

		System.out.println("Com size: " + compSize + ", totSize:" + totSize);

//		dc.dump("Zip summary");

	}

	public static void process(String kind) throws Exception {
		File dir = new File(EDGAR_ROOT, kind);

		index = new HashMap<>();
		process(dir);

		if ( !index.isEmpty() ) {
			try (FileWriter out = new FileWriter("work/Edgarindex.json")) {
				JSONValue.writeJSONString(index, out);
			}
		}

		try (PrintWriter sum = new PrintWriter("work/EdgarSummary.txt")) {
			for (Map.Entry<Object, Long> e : dc) {
				sum.println(e.getKey() + "\t" + e.getValue());
			}
		}

	}

	public static void process(File dir) throws Exception {
		for (File f : dir.listFiles()) {
			if ( f.isDirectory() ) {
				process(f);
			} else if ( f.getName().toLowerCase().endsWith(".json") ) {
				logCount();
				try {
//					String id = DustUtils.cutPostfix(f.getName(), ".");

					Map report = (Map) parser.parse(new FileReader(f));

					if ( report.isEmpty() ) {
						dc.add("Empty\t\t\t");
					} else {
						for (Object he : report.entrySet()) {
							Object k = ((Map.Entry) he).getKey();

							if ( "facts".equals(k) ) {
								Map<String, Object> tax = (Map) ((Map.Entry) he).getValue();
								for (Map.Entry<String, Object> te : tax.entrySet()) {
									String t = te.getKey();

									dc.add("Taxonomy\t" + t + "\t\t");

									Map<String, Object> concepts = (Map) ((Map.Entry) te).getValue();
									for (Map.Entry<String, Object> ce : concepts.entrySet()) {
										String ck = ce.getKey();
										String c = t + "\t" + ck;

										String ct = "?";

//										dc.add("Concept\t" + c);

										Object units = ((Map<String, Object>) ce.getValue()).get("units");

										if ( null == units ) {
											dc.add("NO_UNITS" + c);
										} else {

//											Dust.access(index, MindAccess.Set, id, t, ck, KEY_ADD);

											for (Object va : ((Map) units).entrySet()) {
												Object uk = ((Map.Entry) va).getKey();
												dc.add("Unit\t\t" + uk + "\t");
												for (Object v : (Collection) ((Map.Entry) va).getValue()) {
													for (Object ok : ((Map) v).keySet()) {
														dc.add("ValKey\t\t" + ok + "\t");
													}

													Object vv = ((Map) v).get("val");

													if ( vv instanceof String ) {
														ct = "str";
													} else if ( vv instanceof Number ) {
														ct = "num";
													} else if ( vv instanceof Boolean ) {
														ct = "bool";
													}
												}
											}
										}

										dc.add("Concept\t" + c + "\t" + ct);

									}

								}
							} else {
								dc.add("Head\t\t" + k + "\t");
							}
						}
					}
				} catch (Throwable e) {
					DustException.swallow(e, "Reading file", f.getCanonicalPath());
				}

			}
		}
	}

	public static void load(File target, String... concepts) throws Exception {

		File dir = new File(EDGAR_ROOT, "companyfacts");
		index = (Map) parser.parse(new FileReader("work/Edgarindex.json"));

		Set<String> toLoad = new TreeSet<>();
		for (String cc : concepts) {
			String[] split = cc.split(":");
			Collection<String> list = Dust.access(index, MindAccess.Get, null, split[0], split[1]);
			toLoad.addAll(list);
		}

		if ( toLoad.isEmpty() ) {
			return;
		}

		Pattern pt = Pattern.compile("CIK(0*)(?<code>\\d+)");

		try (PrintWriter w = new PrintWriter(target)) {

			w.println("Report\tEntity\tTaxonomy\tConcept\tStart\tEnd\tInstant\tUnit\tValue");

			Map<String, Object> fact = new HashMap<>();

			for (String id : toLoad) {
				String code = id;

				Matcher m = pt.matcher(code);
				if ( m.matches() ) {
					code = m.group("code");
				}

				File d = new File(dir, DustUtilsFile.getHashForID(id));
				File f = new File(d, id + ".json");

				if ( f.exists() ) {
					System.out.println(id + ": " + f.getCanonicalPath());

					Map report = (Map) parser.parse(new FileReader(f));

					for (String cc : concepts) {
						String[] split = cc.split(":");

						Map<String, Collection<Map>> units = Dust.access(report, MindAccess.Peek, null, "facts", split[0], split[1], "units");

						if ( null == units ) {
							continue;
						}

						for (Map.Entry<String, Collection<Map>> ue : units.entrySet()) {
							String uk = ue.getKey();
							for (Map v : ue.getValue()) {
								fact.clear();
								fact.putAll(v);

//						"start": "2022-01-01",
//						"end": "2022-03-31",
//						"val": 554107,
//						"accn": "0001410578-22-001639",
//						"fy": 2022,
//						"fp": "Q1",
//						"form": "10-Q",
//						"filed": "2022-05-16",
//						"frame": "CY2022Q1"

								if ( !fact.containsKey("start") ) {
									fact.put("instant", fact.remove("end"));
								}

								Object val = fact.get("val");

								if ( val instanceof String ) {
									val = DustUtils.csvEscape((String) val, true);
								}

								StringBuilder sbLine = DustUtils.sbAppend(null, "\t", true, code + "/" + fact.get("accn"), id, split[0], split[1], fact.get("start"), fact.get("end"), fact.get("instant"), uk, val);

								w.println(sbLine);

							}
						}
					}
				}
			}
		}
	}


//public void addPath() throws Exception {
//	pm = new DustUtils.ProcessMonitor("Add path", 1000);
//
//	DustUtils.TableReader tr = null;
//
//	File dir = new File(edgarRoot, "submissions");
//	File f = new File(dir, "SubmissionIndex2.csv");
//	PrintWriter pwHead = new PrintWriter(f);
//	pwHead.println(EDGAR_HEAD_HEADER);
////	pwHead.print(EDGAR_HEAD_HEADER);
////	pwHead.println("\t__PathPrefix");
//
//	try (BufferedReader br = new BufferedReader(new FileReader(fSubmissionIndex))) {
//		for (String line; (line = br.readLine()) != null;) {
//
//			if ( pm.step() ) {
//				pwHead.flush();
//			}
//
//			String[] row = line.split("\t");
//			if ( null == tr ) {
//				tr = new DustUtils.TableReader(row);
//			} else {
//				String cik = tr.get(row, EdgarHeadFields.cik.name());
//				// CIK0000877443
//				String pathPrefix = String.format("CIK%10s", cik).replace(' ', '0');
//				pathPrefix = DustUtilsFile.getHashForID(pathPrefix) + "/" + pathPrefix;
//
////				File fj = new File(dir, pathPrefix + ".json");
////				if ( !fj.isFile() ) {
////					DustUtils.breakpoint();
////				}
//
//				pwHead.print(line);
//				pwHead.print("\t");
//				pwHead.println(pathPrefix);
//			}
//		}
//	}
//
//	pwHead.flush();
//	pwHead.close();
//
//	System.out.println(pm);
//}
}
