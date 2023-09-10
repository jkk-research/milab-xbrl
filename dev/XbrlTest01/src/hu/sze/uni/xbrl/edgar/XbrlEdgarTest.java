package hu.sze.uni.xbrl.edgar;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.json.simple.parser.JSONParser;

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

	static void unzip(String kind) throws Exception {

		File zipFile = new File(EDGAR_ROOT, kind + ".zip");
		File dir = new File(EDGAR_ROOT, kind);

//		File zipFile = new File("/Users/lkedves/work/xbrl/data/sources/edgar/submissions.zip");

		long compSize = 0;
		long totSize = 0;

		try (ZipFile zf = new ZipFile(zipFile)) {

			for (Enumeration<ZipArchiveEntry> ee = zf.getEntries(); ee.hasMoreElements();) {
				ZipArchiveEntry ze = ee.nextElement();

				if ( !ze.isDirectory() ) {
					String fName = ze.getName();
					String id = DustUtils.cutPostfix(fName, ".");
					File d = new File(dir, DustUtilsFile.getHashForID(id));
					File f = new File(d, fName);

					if ( !f.exists() ) {
						XbrlTestPortalUtils.unzipEntry(zf, ze, f);
					}
				}

//				dc.add(ze.isDirectory() ? "Dir" : "File");

				logCount();

//				compSize += ze.getCompressedSize();
//				totSize += ze.getSize();
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

	public static void process(String kind) throws Exception {
		File dir = new File(EDGAR_ROOT, kind);

		process(dir);
	}

	public static void process(File dir) throws Exception {
		for (File f : dir.listFiles()) {
			if ( f.isDirectory() ) {
				process(f);
			} else if ( f.getName().toLowerCase().endsWith(".json") ) {
				logCount();
				try {
					Map report = (Map) parser.parse(new FileReader(f));

					if ( report.isEmpty() ) {
						dc.add("Empty");
					} else {
						for (Object he : report.entrySet()) {
							Object k = ((Map.Entry) he).getKey();

							if ( "facts".equals(k) ) {
								Map<String, Object> tax = (Map) ((Map.Entry) he).getValue();
								for (Map.Entry<String, Object> te : tax.entrySet()) {
									String t = te.getKey();
									
									dc.add("Taxonomy\t" + t);

									Map<String, Object> concepts = (Map) ((Map.Entry) te).getValue();
									for (Map.Entry<String, Object> ce : concepts.entrySet()) {
										String c = t + "\t" + ce.getKey();

										dc.add("Concept\t" + c);

										Object units = ((Map<String, Object>) ce.getValue()).get("units");

										if ( null == units ) {
											dc.add("NO_UNITS" + c);
										} else {
											for (Object va : ((Map) units).values()) {
												for (Object v : (Collection) va) {
													for (Object ok : ((Map) v).keySet()) {
														dc.add("ValKey\t" + ok);
													}
												}
											}
										}
									}

								}
							} else {
								dc.add("Head\t" + k);
							}
						}
					}
				} catch (Throwable e) {
					DustException.swallow(e, "Reading file", f.getCanonicalPath());
				}

			}
		}

	}

	public static void main(String[] args) throws Exception {
		long t = System.currentTimeMillis();

//		unzip("companyfacts");
		process("companyfacts");

		try (PrintWriter sum = new PrintWriter("work/EdgarSummary.txt")) {
			for (Map.Entry<Object, Long> e : dc) {
				sum.println(e.getKey() + "\t" + e.getValue());
			}
		}

//		sum.close();

		System.out.println("Count: " + count + ", Time " + (System.currentTimeMillis() - t) + " msec.");

	}
}
