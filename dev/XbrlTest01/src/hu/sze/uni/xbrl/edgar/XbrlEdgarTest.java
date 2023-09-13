package hu.sze.uni.xbrl.edgar;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
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

	private static int count = 0;

	static void unzip(String kind, boolean unzip) throws Exception {

		File zipFile = new File(EDGAR_ROOT, kind + ".zip");
		File dir = new File(EDGAR_ROOT, kind);

//		File zipFile = new File("/Users/lkedves/work/xbrl/data/sources/edgar/submissions.zip");

		long compSize = 0;
		long totSize = 0;

		try (ZipFile zf = new ZipFile(zipFile)) {

			for (Enumeration<ZipArchiveEntry> ee = zf.getEntries(); ee.hasMoreElements();) {
				ZipArchiveEntry ze = ee.nextElement();

				if ( unzip && !ze.isDirectory() ) {
					String fName = ze.getName();
					String id = DustUtils.cutPostfix(fName, ".");
					File d = new File(dir, DustUtilsFile.getHashForID(id));
					File f = new File(d, fName);

					if ( !f.exists() ) {
						XbrlTestPortalUtils.unzipEntry(zf, ze, f);
					}
				}

				dc.add(ze.isDirectory() ? "Dir" : "File");

				logCount();

				compSize += ze.getCompressedSize();
				totSize += ze.getSize();
//				zf.getInputStream(ze);

//			if ( (857400 < count) && (count < 857500) ){
//			System.out.println(fName + ": " + ze.getCompressedSize()  + ": " + ze.getSize() );
//		}

			}
		}

		System.out.println("Com size: " + compSize + ", totSize:" + totSize);

	}

	public static void logCount() {
		if ( 0 == (++count % 100) ) {
			System.out.println("Count " + count);
		}
	}

	static Map index;

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
					String id = DustUtils.cutPostfix(f.getName(), ".");

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

	public static void main(String[] args) throws Exception {
		Dust.main(args);

		long t = System.currentTimeMillis();

		unzip("submissions", false);
//		unzip("companyfacts", false);
//		process("companyfacts");

//		File f = new File("work/EdgarTestExport.csv");
//		load(f, "us-gaap:AccountsPayableOther", "ifrs-full:AccountingProfit");

//		sum.close();

		System.out.println("Count: " + count + ", Time " + (System.currentTimeMillis() - t) + " msec.");

	}

//private static class EdgarInfoCollector implements ContentHandler {
//	
//	Collection<Map> target;
//	
//	public void setTarget(Collection<Map> target) {
//		this.target = target;
//	}
//
//	@Override
//	public void startJSON() throws ParseException, IOException {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void endJSON() throws ParseException, IOException {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public boolean startObject() throws ParseException, IOException {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public boolean endObject() throws ParseException, IOException {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public boolean startObjectEntry(String key) throws ParseException, IOException {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public boolean endObjectEntry() throws ParseException, IOException {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public boolean startArray() throws ParseException, IOException {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public boolean endArray() throws ParseException, IOException {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public boolean primitive(Object value) throws ParseException, IOException {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//}

//private static EdgarInfoCollector EC = new EdgarInfoCollector();
//
//public static void collectInfo(String cik, File fJson, Collection<Map> target) throws Exception {
//	try (FileReader jr = new FileReader(fJson)) {
//		parser.parse(jr, EC);
//	}
//}
}
