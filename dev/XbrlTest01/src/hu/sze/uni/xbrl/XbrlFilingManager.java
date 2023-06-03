package hu.sze.uni.xbrl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.simple.parser.JSONParser;

@SuppressWarnings({ "rawtypes" })
public class XbrlFilingManager implements XbrlConsts {
	public static final String XBRL_ORG_ADDR = "https://filings.xbrl.org";
	public static final String ENTITY_NAME = "__EntityName";
	public static final String ENTITY_ID = "__EntityId";
	public static final String LOCAL_DIR = "__LocalDir";
	public static final String REPORT_YEAR = "__ReportYear";
	public static final String REPORT_ID = "__ReportId";
	public static final String REPORT_FILE = "__ReportFile";

	private static final String[] URL_ATTS = { "fxo_id", "json_url", "report_url", "package_url" };

	// "https://filings.xbrl.org/api/filings?include=entity&filter=%5B%5D&sort=-date_added&page%5Bsize%5D=200&page%5Bnumber%5D=1",
	// https://filings.xbrl.org/api/filings?include=entity,language&filter=%5B%5D&sort=-date_added&page%5Bsize%5D=20&page%5Bnumber%5D=1&_=1684136794691
// https://filings.xbrl.org/api/filings?include=entity,language&filter=%5B%7B%22name%22%3A%22country%22%2C%22op%22%3A%22eq%22%2C%22val%22%3A%22HU%22%7D%5D&sort=-date_added&page%5Bsize%5D=20&page%5Bnumber%5D=1&_=1684747335434
// https://www.random.org/integer-sets/?sets=1&num=100&min=1&max=6000&commas=on&sort=on&order=index&format=plain&rnd=new
// https://filings.xbrl.org/api/filings?include=entity,language&filter=%5B%7B%22name%22%3A%22country%22%2C%22op%22%3A%22eq%22%2C%22val%22%3A%22HU%22%7D%5D&sort=-date_added&page%5Bsize%5D=60&page%5Bnumber%5D=1
	File repoRoot;

	List<Map> filings;
	Map<String, Map> entities;

	Pattern PT_FXO = Pattern.compile("(?<eid>\\w+)-(?<year>\\d+)-(\\d+-\\d+)-(?<extra>.*)");

	public XbrlFilingManager(String repoPath) throws Exception {
		repoRoot = new File(repoPath);

		JSONParser p = new JSONParser();

//		File fAll = new File(repoRoot, "ApiResponse.json");
//		File fAll = new File(repoRoot, "filings.all.json");
		File fAll = new File(repoRoot, "FilingsHu.json");

		if ( !fAll.exists() ) {
			throw new RuntimeException("Missing all " + fAll.getAbsolutePath());
		}
		Object allFilings = p.parse(new FileReader(fAll));

		filings = XbrlUtils.access(allFilings, AccessCmd.Peek, null, "data");

		entities = new TreeMap<>();
		List<Map> included = XbrlUtils.access(allFilings, AccessCmd.Peek, null, "included");
		for (Map i : included) {
			if ( "entity".equals(XbrlUtils.access(i, AccessCmd.Peek, null, "type")) ) {
				String id = XbrlUtils.access(i, AccessCmd.Peek, null, "id");
				entities.put(id, XbrlUtils.access(i, AccessCmd.Peek, null, "attributes"));
			}
		}

		Map<String, Integer> counts = new TreeMap<>();
		int mc = 0;

		for (Map f : filings) {
			String eRef = XbrlUtils.access(f, AccessCmd.Peek, null, "relationships", "entity", "data", "id");
			Map eAtts = entities.get(eRef);
			Map fAtts = XbrlUtils.access(f, AccessCmd.Peek, null, "attributes");
			XbrlUtils.access(fAtts, AccessCmd.Set, eAtts.get("name"), ENTITY_NAME);
			XbrlUtils.access(fAtts, AccessCmd.Set, eAtts.get("identifier"), ENTITY_ID);

			String xoid = XbrlUtils.access(fAtts, AccessCmd.Peek, null, "fxo_id");
			Matcher m = PT_FXO.matcher(xoid);

			if ( m.matches() ) {
				++mc;

				String year = m.group("year");
				String eid = m.group("eid");
				String extra = m.group("extra");
				
				XbrlUtils.access(fAtts, AccessCmd.Set, year, REPORT_YEAR);

				StringBuilder sb = XbrlUtils.sbAppend(null, File.separator, true, "reports", year, XBRL_SOURCE_FILINGS, XbrlUtils.getHashName(eid), extra);
				XbrlUtils.access(fAtts, AccessCmd.Set, sb.toString(), LOCAL_DIR);

				sb = XbrlUtils.sbAppend(null, IDSEP, true, XBRL_ENTITYID_LEI, eid, year, XBRL_SOURCE_FILINGS, extra);
				XbrlUtils.access(fAtts, AccessCmd.Set, sb.toString(), REPORT_ID);
			}

			for (String u : URL_ATTS) {
				String addr = XbrlUtils.access(fAtts, AccessCmd.Peek, null, u);

				if ( null != addr ) {
					counts.put(u, counts.getOrDefault(u, 0) + 1);
				}
			}
		}

		System.out.println("Total count: " + filings.size() + ", fxo match: " + mc + ", url attribute counts: " + counts);
	}

	public Map<String, String> getAllEntities(Map<String, String> target, String filter) {
		if ( null == target ) {
			target = new TreeMap<>();
		} else {
			target.clear();
		}

		for (Map.Entry<String, Map> ee : entities.entrySet()) {
			Map ea = ee.getValue();
			String name = (String) ea.get("name");

			if ( (null == filter) || (-1 != name.toLowerCase().indexOf(filter.toLowerCase())) ) {
				target.put((String) ea.get("identifier"), name);
			}
		}

		return target;
	}

	public Set<String> getEntityNames(Set<String> target, String filter) {
		if ( null == target ) {
			target = new TreeSet<>();
		} else {
			target.clear();
		}

		for (Map.Entry<String, Map> ee : entities.entrySet()) {
			Map ea = ee.getValue();
			String name = (String) ea.get("name");
			if ( (null == filter) || (-1 != name.toLowerCase().indexOf(filter.toLowerCase())) ) {
				target.add(name);
			}
		}

		return target;
	}

	public List<Map> getFilings(Map<String, Object> match, List<Map> target) {
		if ( null == target ) {
			target = new ArrayList<>();
		} else {
			target.clear();
		}

		for (Map f : filings) {
			Map atts = XbrlUtils.access(f, AccessCmd.Peek, null, "attributes");

			for (Map.Entry<String, Object> me : match.entrySet()) {
				Object cond = me.getValue();
				Object att = atts.get(me.getKey());
				boolean ok = false;

				if ( null != att ) {
					if ( att instanceof String ) {
						ok = (-1 != ((String) att).toLowerCase().indexOf(((String) cond).toLowerCase()));
					} else {
						ok = att.equals(cond);
					}
				}

				if ( !ok ) {
					atts = null;
					break;
				}
			}

			if ( null != atts ) {
				target.add(f);
			}
		}

		return target;
	}

	public File getReport(Map filing, XbrlReportType repType) throws Exception {
		File ret = null;

		String repUrl = XbrlUtils.access(filing, AccessCmd.Peek, null, "attributes", (repType == XbrlReportType.Json) ? "json_url" : "package_url");

		if ( null == repUrl ) {
			return null;
		}

//		String eId = XbrlUtils.access(filing, AccessCmd.Peek, null, "attributes", ENTITY_ID);
//		File dir = XbrlUtils.getHashDir(repoRoot, eId);
		String repDir = XbrlUtils.access(filing, AccessCmd.Peek, null, "attributes", LOCAL_DIR);
		File dir = new File(repoRoot, repDir);
		dir.mkdirs();

		String url = XBRL_ORG_ADDR + repUrl;
		int sep = url.lastIndexOf('/');
		String fName = url.substring(sep + 1);
		String retFileName = fName;

		if ( repType == XbrlReportType.Package ) {
			sep = fName.lastIndexOf('.');
			retFileName = fName.substring(0, sep);
		}

		ret = new File(dir, retFileName);

		if ( !ret.exists() ) {
			File remoteFile = new File(dir, fName);
			if ( !remoteFile.exists() ) {
				url = url.replace(" ", "%20");
				System.out.println("Accessing file " + fName + " from URL: " + url);
				try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream()); FileOutputStream fileOutputStream = new FileOutputStream(remoteFile)) {
					byte dataBuffer[] = new byte[1024];
					int bytesRead;
					while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
						fileOutputStream.write(dataBuffer, 0, bytesRead);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Resolving file from local cache " + fName);
			}

			if ( repType == XbrlReportType.Package ) {
				System.out.println("Unzipping " + remoteFile.getName() + " to: " + ret.getName());
				try {
					XbrlUtils.unzip(remoteFile, ret);
				} catch (Throwable e) {
					e.printStackTrace(System.err);
				}
			} else {
				ret = remoteFile;
			}
		} else {
			System.out.println("Resolving file from local cache " + fName);
		}

		return ret;
	}

	public static void main(String[] args) throws Exception {
		long t = System.currentTimeMillis();
		String home = System.getProperty("user.home");
		XbrlFilingManager fm = new XbrlFilingManager(home + "/work/xbrl/data");

		for (String s : fm.getEntityNames(null, "group")) {
			System.out.println(s);
		}

		Map<String, Object> match = new HashMap<>();

//		match.put(ENTITY_NAME, "Budapesti Ingatlan Hasznos");
		match.put("country", "HU");

		List<Map> fl = fm.getFilings(match, null);

		Map<XbrlReportFormat, File> repFiles = new TreeMap<>();
		
		Map<Map, File> repToRead = new HashMap<>();
		ArrayList<File> repDirFail = new ArrayList<>();

		for (Map lm : fl) {
			XbrlUtils.dump(",", true, XbrlUtils.access(lm, AccessCmd.Peek, null, "attributes", ENTITY_NAME), XbrlUtils.access(lm, AccessCmd.Peek, null, "attributes", REPORT_ID));

			File ff = fm.getReport(lm, XbrlReportType.Package);
			System.out.println(ff.getCanonicalPath());

			File repDir = XbrlUtils.searchByName(ff, "reports");
			
			if ( null == repDir ) {
				// old reports had no "reports" subfolder
				repDir = ff;
			}

			if ( (null == repDir) || !repDir.isDirectory() ) {
				System.out.println("Report dir not found in " + ff.getName());
				repDirFail.add(ff);
			} else {
				repFiles.clear();

				for (File fRep : repDir.listFiles()) {
					if ( fRep.isFile() ) {
						String fn = fRep.getName().toUpperCase();
						int d = fn.lastIndexOf('.');
						String type = fn.substring(d + 1).toUpperCase();
						
						try {
							repFiles.put(XbrlReportFormat.valueOf(type), fRep);
						} catch ( Throwable e ) {
							
						}

					}
				}
				
				if ( repFiles.isEmpty() ) {
					System.out.println("Report file not found in " + ff.getName());
				} else {
					File fRep = repFiles.values().iterator().next();
					
					System.out.println("Report file to read " + fRep.getName());
					repToRead.put(lm, fRep);

				}
			}
		}

		System.out.println("File count: " + repToRead.size());
		System.out.println("Fail count: " + repDirFail.size());
		
		
		System.out.println("\n\nReport dir fail ");

		for ( File f : repDirFail ) {
			System.out.println("   " + f.getAbsolutePath());
		}

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser xmlParser = factory.newSAXParser();

		XbrlTagCoverage listener = new XbrlTagCoverage();
		listener.readTaxonomy("TaxonomyDefs.xlsx");

		XbrlHandlerInnerXhtml xhtmlHandler = new XbrlHandlerInnerXhtml();
		xhtmlHandler.setListener(listener);
		
		System.out.println("\n\nReport to read ");

		for ( Map.Entry<Map, File>  ee : repToRead.entrySet() ) {
			Map key = ee.getKey();
			File f = ee.getValue();			
			listener.setCurrentFiling(key, f);
			xmlParser.parse(f, xhtmlHandler);
		}
		
		System.out.println("Dim keys" + listener.cntDimKeys);
				
		listener.writeExcel("TaxonomyCoverageHu.xlsx");
		
		System.out.println("Process complete in " + (System.currentTimeMillis() - t) + " msec.");
	}
}
