package hu.sze.uni.xbrl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import hu.sze.milab.dust.DustException;

@SuppressWarnings({ "rawtypes" })
public class XbrlFilingManager implements XbrlConsts {
	public static final String XBRL_ORG_ADDR = "https://filings.xbrl.org";
	public static final String ENTITY_NAME = "__EntityName";
	public static final String ENTITY_ID = "__EntityId";
	public static final String LOCAL_DIR = "__LocalDir";
	public static final String REPORT_DATE = "__ReportDate";
	public static final String REPORT_ID = "__ReportId";
	public static final String REPORT_FILE = "__ReportFile";

	private static final FilenameFilter FF_JSON = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(".json");
		}
	};
	private static final String FMT_API = XBRL_ORG_ADDR + "/api/filings?include=entity,language&sort=-date_added&page%5Bsize%5D={0,number,#}&page%5Bnumber%5D=1";

//https://www.random.org/integer-sets/?sets=1&num=100&min=0&max=6939&commas=on&sort=on&order=index&format=plain&rnd=new

	File repoRoot;

	JSONParser parser = new JSONParser();

	Map<String, Map> entities;
	Map<String, Map> reports = new TreeMap<>();

	Set<Map> downloaded = new HashSet<>();
	boolean downloadOnly = true;

	Pattern PT_FXO = Pattern.compile("(?<eid>\\w+)-(?<date>\\d+-\\d+-\\d+)-(?<extra>.*)");

	public XbrlFilingManager(String repoPath, boolean doUpdate) throws Exception {
		repoRoot = new File(repoPath);

		File srcRoot = new File(repoRoot, "sources/xbrl.org");

		if ( !srcRoot.exists() ) {
			srcRoot.mkdirs();
		}

		File updates = new File(srcRoot, "updates");
		if ( !updates.exists() ) {
			updates.mkdirs();
		}

		for (File resp : updates.listFiles(FF_JSON)) {
			loadReports(resp);
		}

		if ( doUpdate ) 
		{
			File fPing = new File(srcRoot, "ping.json");

			long dPing = TimeUnit.MILLISECONDS.toDays(fPing.exists() ? fPing.lastModified() : 0);
			long dNow = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis());

			if ( 0 < (dNow - dPing) ) {
				String url = MessageFormat.format(FMT_API, 1);
				XbrlUtils.download(url, fPing);

				Object ret = parser.parse(new FileReader(fPing));
				long count = XbrlUtils.access(ret, AccessCmd.Peek, 0L, "meta", "count");
				long diff = count - reports.size();

				if ( 0 < diff ) {
					SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd");
					String fName = fmt.format(new Date(System.currentTimeMillis())) + ".json";
					File f = new File(updates, fName);
					String u2 = MessageFormat.format(FMT_API, diff + 1);
					XbrlUtils.download(u2, f);
					loadReports(f);
				}
			}
		}
	}

	public void loadReports(File fAll) throws IOException, ParseException, FileNotFoundException {
		Object allFilings = parser.parse(new FileReader(fAll));

		System.out.println("Reading API response: " + fAll.getName());

		long count = XbrlUtils.access(allFilings, AccessCmd.Peek, 0L, "meta", "count");
		ArrayList<Map> filings = XbrlUtils.access(allFilings, AccessCmd.Peek, null, "data");

		entities = new TreeMap<>();
		List<Map> included = XbrlUtils.access(allFilings, AccessCmd.Peek, null, "included");
		for (Map i : included) {
			if ( "entity".equals(XbrlUtils.access(i, AccessCmd.Peek, null, "type")) ) {
				String id = XbrlUtils.access(i, AccessCmd.Peek, null, "id");
				entities.put(id, XbrlUtils.access(i, AccessCmd.Peek, null, "attributes"));
			}
		}

//		System.out.println("Repeated ID\tOld fxo_id\tNew id\tNew fxo_id");

		for (Map filing : filings) {
			String eRef = XbrlUtils.access(filing, AccessCmd.Peek, null, "relationships", "entity", "data", "id");
			Map eAtts = entities.get(eRef);
			Map fAtts = XbrlUtils.access(filing, AccessCmd.Peek, null, "attributes");
			XbrlUtils.access(fAtts, AccessCmd.Set, eAtts.get("name"), ENTITY_NAME);
			XbrlUtils.access(fAtts, AccessCmd.Set, eAtts.get("identifier"), ENTITY_ID);

			String xoid = XbrlUtils.access(fAtts, AccessCmd.Peek, null, "fxo_id");
			Matcher m = PT_FXO.matcher(xoid);

			if ( m.matches() ) {
				String date = m.group("date");
				String eid = m.group("eid");
				String extra = m.group("extra");

				XbrlUtils.access(fAtts, AccessCmd.Set, date, REPORT_DATE);

				StringBuilder sb = XbrlUtils.sbAppend(null, File.separator, true, "reports", date.substring(0, 4), XBRL_SOURCE_FILINGS, XbrlUtils.getHashName(eid), extra);
				String localDir = sb.toString();
				XbrlUtils.access(fAtts, AccessCmd.Set, localDir, LOCAL_DIR);
				if ( new File(repoRoot, localDir).exists() ) {
//					System.out.println("Downloaded: " + localDir);
					downloaded.add(filing);
				}

				sb = XbrlUtils.sbAppend(null, IDSEP, true, XBRL_ENTITYID_LEI, eid, date, XBRL_SOURCE_FILINGS, extra);
				String internalId = sb.toString();
				XbrlUtils.access(fAtts, AccessCmd.Set, internalId, REPORT_ID);

				Map old = reports.get(internalId);
				if ( null != old ) {
					System.out.println(old.get("id") + "\t" + XbrlUtils.access(old, AccessCmd.Peek, null, "attributes", "fxo_id") + "\t" + filing.get("id") + "\t" + xoid);
				} else {
					reports.put(internalId, filing);
				}
			}
		}

		System.out.println("Returned count: " + count + ", size of filings: " + filings.size() + ", local report count: " + reports.size() + ", downloaded: " + downloaded.size());
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

	public ArrayList<Map> getFilings(Map<String, Object> match, ArrayList<Map> target) {
		if ( null == target ) {
			target = new ArrayList<>();
		} else {
			target.clear();
		}

		for (Map f : reports.values()) {
			Map atts = XbrlUtils.access(f, AccessCmd.Peek, null, "attributes");

			if ( null != match ) {
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
			}

			if ( null != atts ) {
				target.add(f);
			}
		}

		return target;
	}
	
	int segmentSize = 10;
	int downloadLimit = 1000;
	long tsStart = -1;
	long tsSeg = -1;
	
	long sizeAll = 0;
	long sizeSeg = 0;

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
				
				if ( -1 == tsSeg ) {
					tsSeg = System.currentTimeMillis();
					if ( -1 == tsStart ) {
						tsStart = tsSeg;
					}
				}
				
				XbrlUtils.download(url, remoteFile);
				--downloadLimit;
				
				long s = remoteFile.length();
				sizeAll += s;
				sizeSeg += s;
				if ( 0 == (downloadLimit % segmentSize )) {
					long ts = System.currentTimeMillis();
					System.out.println("Remaining downloads " + downloadLimit + 
							", segment speed: " + (sizeSeg / (ts - tsSeg)) + 
							", total speed: " + (sizeAll / (ts - tsStart))
						);
					
					tsSeg = -1;
					sizeSeg = 0;
				}
				return null;
			} else {
				System.out.println("Resolving file from local cache " + fName);
			}
			
			if ( downloadOnly ) {
				return null;
			}

			if ( repType == XbrlReportType.Package ) {
				System.out.println("Unzipping " + remoteFile.getName() + " to: " + ret.getName());
				try {
					XbrlUtils.unzip(remoteFile, ret);
				} catch (Throwable e) {
					System.err.println("Unzip error with file " + remoteFile);
					e.printStackTrace(System.err);
				}
			} else {
				ret = remoteFile;
			}
		} else {
//			System.out.println("Resolving file from local cache " + fName);
		}

		return ret;
	}

	public static void main(String[] args) throws Exception {
		long t = System.currentTimeMillis();
		String home = System.getProperty("user.home");
		XbrlFilingManager fm = new XbrlFilingManager(home + "/work/xbrl/data", false);
		
		Map<String, Object> match = new HashMap<>();

//	match.put("country", "HU");
		fm.downloadOnly = false;
		match.put(ENTITY_NAME, "aviva");
		List<Map> fl = fm.getFilings(match, null);

//		Set<Map> fl = new HashSet<>();
//
//		ArrayList<Map> flAll = fm.getFilings(match, null);
//		String content = new String(Files.readAllBytes(Paths.get("random.txt")), StandardCharsets.UTF_8);
//
//		String[] indexes = content.split(", ");
//
//		for (String is : indexes) {
//			Map lm = flAll.get(Integer.parseInt(is));
//
////			File ff = fm.getReport(lm, XbrlReportType.Package);
////			System.out.println(ff.getCanonicalPath());
//			fl.add(lm);
//		}

//		Set<Map> fl = fm.downloaded;
//		List<Map> fl = fm.getFilings(null, null);

		if ( fl.isEmpty() ) {
			return;
		}

		Map<XbrlReportFormat, File> repFiles = new TreeMap<>();

		Map<Map, File> repToRead = new HashMap<>();
		ArrayList<File> repDirFail = new ArrayList<>();

		for (Map lm : fl) {
//			XbrlUtils.dump(",", true, XbrlUtils.access(lm, AccessCmd.Peek, null, "attributes", ENTITY_NAME), XbrlUtils.access(lm, AccessCmd.Peek, null, "attributes", REPORT_ID));

			File ff = fm.getReport(lm, XbrlReportType.Package);
			if ( 0 >= fm.downloadLimit ) {
				DustException.wrap(null, "Download limit exceeded");
			}
			if ( null == ff ) {
				continue;
			}
//			System.out.println(ff.getCanonicalPath());

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
						} catch (Throwable e) {
//							System.out.println("wtf? " + ff.getName());
						}

					}
				}

				if ( repFiles.isEmpty() ) {
//					System.out.println("Report file not found in " + ff.getName());
					repDirFail.add(ff);
				} else {
					File fRep = repFiles.values().iterator().next();

//					System.out.println("Report file to read " + fRep.getName());
					repToRead.put(lm, fRep);

				}
			}
		}

		System.out.println("File count: " + repToRead.size());
		System.out.println("Fail count: " + repDirFail.size());

		System.out.println("\n\nReport dir fail ");

		for (File f : repDirFail) {
			System.out.println("   " + f.getAbsolutePath());
		}

//		XbrlTagExport listener = new XbrlTagExport();
//		XbrlTagCoverage listener = new XbrlTagCoverage();
//		listener.readTaxonomy("TaxonomyDefs.xlsx");

//		XbrlReportLoader loader = new XbrlReportLoader(listener);

		XbrlReportLoaderDOM loader = new XbrlReportLoaderDOM();

		for (Map.Entry<Map, File> ee : repToRead.entrySet()) {
//			Map key = ee.getKey();
			File f = ee.getValue();
//			listener.setCurrentFiling(key, f);
			try {
				loader.load(f);
			} catch (Throwable e) {
				System.err.println("Parse error with file " + f);
				e.printStackTrace(System.err);
			}
		}
		
		loader.dump();

//		System.out.println("Dim keys" + listener.cntDimKeys);
//	listener.writeExcel("TaxonomyCoverageHu.xlsx");
//		listener.writeExcel("TaxonomyCoverageAll.xlsx");
//		listener.writeExcel("TaxonomyCoverageBanks.xlsx");

		System.out.println("Process complete in " + (System.currentTimeMillis() - t) + " msec.");
	}
}
