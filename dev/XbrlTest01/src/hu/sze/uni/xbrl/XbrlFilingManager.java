package hu.sze.uni.xbrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustException;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtilsConsts;
import hu.sze.milab.dust.utils.DustUtilsData;
import hu.sze.uni.xbrl.portal.XbrlTestPortalUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlFilingManager implements XbrlConstsEU, DustUtilsConsts {
	public static final String ALL_REPORTS = "allReports.json";
//	public static final String OVERRIDE = "filings.xbrl.org.override.json";

	public static final String XBRL_ORG_ADDR = "https://filings.xbrl.org";

	private class FactIter implements Iterable<String[]>, Iterator<String[]> {
		BufferedReader br;
		String repId;
		String sep;

		String line;
		int row;

		public FactIter(String repId) throws Exception {
			this(repId, "\t");
		}

		public FactIter(String repId, String sep) throws Exception {
			row = 1;
			this.sep = sep;
			this.repId = repId;

			File csvVal = getFactFile(repId);
			br = new BufferedReader(new FileReader(csvVal));
			line = br.readLine();// skip head!
			line = br.readLine();
		}

		@Override
		public boolean hasNext() {
			return !DustUtils.isEmpty(line);
		}

		@Override
		public String[] next() {
			String[] data = line.split(sep);

			try {
				++row;
				line = br.readLine();
			} catch (Throwable e) {
				line = null;
				DustException.wrap(e, repId, row);
			}

			return data;
		}

		@Override
		public Iterator<String[]> iterator() {
			return this;
		}

	}

	private static final FilenameFilter FF_JSON = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(".json");
		}
	};

	private static final String FMT_API = XBRL_ORG_ADDR + "/api/filings?include=entity,language&sort=-date_added&page%5Bsize%5D={0,number,#}&page%5Bnumber%5D=1";

//https://www.random.org/integer-sets/?sets=1&num=100&min=0&max=6939&commas=on&sort=on&order=index&format=plain&rnd=new

	File repoRoot;
	File allReports;

	JSONParser parser = new JSONParser();

//	Map<String, Map> urlOverride;

	Map<String, Map> entities;
	Map<String, Map> reportData = new TreeMap<>();

	Set<Map> downloaded = new HashSet<>();
	boolean downloadOnly = true;
	int downloadLimit = 1000;

	boolean forceReload = false;

	Pattern PT_FXO = Pattern.compile("(?<eid>\\w+)-(?<date>\\d+-\\d+-\\d+)-(?<extra>.*)");

	Map<String, DustUtilsData.TableReader> contentReaders = new HashMap<>();
	Map<String, DustUtilsData.TableReader> headers = new HashMap<>();
	Map<String, ArrayList<String[]>> allFactsByRep;

	DustFileFilter reportFilter = new DustFileFilter(true, StringMatch.EndsWith, "xhtml", "html");

	public XbrlFilingManager(String repoPath, boolean doUpdate) throws Exception {
		this(new File(repoPath), doUpdate);
	}

	public XbrlFilingManager(File repoRoot, boolean doUpdate) throws Exception {
		this.repoRoot = repoRoot;

		String mm = System.getProperty("MinMem", "true");
		allFactsByRep = "true".equalsIgnoreCase(mm) ? null : new HashMap<>();

		forceReload = "true".equalsIgnoreCase(System.getProperty("ForceReload", "false"));
		downloadLimit = Integer.parseInt(System.getProperty("DownloadLimit", "1000"));

//		File override = new File(OVERRIDE);
//		urlOverride = override.isFile() ? (Map<String, Map>) parser.parse(new FileReader(override)) : new HashMap<>();

		File srcRoot = new File(repoRoot, "sources/xbrl.org");

		if ( !srcRoot.exists() ) {
			srcRoot.mkdirs();
		}

		System.out.println("Starting filing manager in folder " + srcRoot.getCanonicalPath() + ((null == allFactsByRep) ? " MinMem mode" : ""));

		File updates = new File(srcRoot, "updates");
		if ( !updates.exists() ) {
			updates.mkdirs();
		}

		allReports = new File(srcRoot, ALL_REPORTS);

		if ( allReports.exists() ) {
			System.out.println("Reading all reports from " + allReports.getCanonicalPath());

			reportData = (Map<String, Map>) parser.parse(new FileReader(allReports));

			if ( null == entities ) {
				entities = new TreeMap<>();
			}

			for (Map.Entry<String, Map> ee : getReportData().entrySet()) {
				Map ea = ee.getValue();

				String en = (String) ea.get(ENTITY_NAME);
				String eid = (String) ea.get(ENTITY_ID);

				Map mapE = entities.get(eid);
				if ( null == mapE ) {
					mapE = new HashMap<>();
					mapE.put("identifier", eid);
					mapE.put("name", en);
					entities.put(eid, mapE);
				}
			}
		} else {
			for (File resp : updates.listFiles(FF_JSON)) {
				loadReports(resp);
			}
		}

		if ( doUpdate ) {
			File fPing = new File(srcRoot, "ping.json");

			long dPing = TimeUnit.MILLISECONDS.toDays(fPing.exists() ? fPing.lastModified() : 0);
			long dNow = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis());

			boolean saveFile = !allReports.exists();

			if ( 0 < (dNow - dPing) ) {
				String url = MessageFormat.format(FMT_API, 1);

				System.out.println("Ping for new data URL: " + url);

				XbrlUtils.download(url, fPing);

				Object ret = parser.parse(new FileReader(fPing));
				long count = XbrlUtils.access(ret, AccessCmd.Peek, 0L, "meta", "count");
				long diff = count - getReportData().size();

				if ( 0 < diff ) {
					SimpleDateFormat fmt = new SimpleDateFormat("YYYY-MM-dd");
					String fName = fmt.format(new Date(System.currentTimeMillis())) + ".json";
					File f = new File(updates, fName);
					String u2 = MessageFormat.format(FMT_API, diff + 1);

					System.out.println("Get update URL: " + u2);

					XbrlUtils.download(u2, f);
					loadReports(f);
					saveFile = true;
				}
			}

			if ( saveFile ) {
				System.out.println("Updating all data: " + allReports.getCanonicalPath());
				FileWriter fw = new FileWriter(allReports);
				JSONValue.writeJSONString(getReportData(), fw);
				fw.flush();
				fw.close();
			}
		}
	}

	public void setDownloadOnly(boolean downloadOnly) {
		this.downloadOnly = downloadOnly;
	}

	public void loadReports(File fAll) throws IOException, ParseException, FileNotFoundException {
		Object allFilings = parser.parse(new FileReader(fAll));

		System.out.println("Reading API response: " + fAll.getName());

		long count = XbrlUtils.access(allFilings, AccessCmd.Peek, 0L, "meta", "count");
		ArrayList<Map> filings = XbrlUtils.access(allFilings, AccessCmd.Peek, null, "data");

		entities = new TreeMap<>();
		Map lang = new TreeMap<>();
		Map langCode = new TreeMap<>();
		List<Map> included = XbrlUtils.access(allFilings, AccessCmd.Peek, null, "included");
		for (Map i : included) {
			String inclType = XbrlUtils.access(i, AccessCmd.Peek, null, "type");
			String id = XbrlUtils.access(i, AccessCmd.Peek, null, "id");

			switch ( inclType ) {
			case "entity":
				entities.put(id, XbrlUtils.access(i, AccessCmd.Peek, null, "attributes"));
				break;
			case "language":
				lang.put(id, XbrlUtils.access(i, AccessCmd.Peek, null, "attributes", "name"));
				langCode.put(id, XbrlUtils.access(i, AccessCmd.Peek, null, "attributes", "code"));
				break;
			}
		}

		for (Map filing : filings) {
			String eRef = XbrlUtils.access(filing, AccessCmd.Peek, null, "relationships", "entity", "data", "id");
			Map eAtts = entities.get(eRef);
			Map fAtts = XbrlUtils.access(filing, AccessCmd.Peek, null, "attributes");
			XbrlUtils.access(fAtts, AccessCmd.Set, eAtts.get("name"), ENTITY_NAME);
			XbrlUtils.access(fAtts, AccessCmd.Set, eAtts.get("identifier"), ENTITY_ID);

			String lRef = XbrlUtils.access(filing, AccessCmd.Peek, null, "relationships", "language", "data", "id");
			if ( null != lRef ) {
				XbrlUtils.access(fAtts, AccessCmd.Set, lang.get(lRef), LANGUAGE);
				XbrlUtils.access(fAtts, AccessCmd.Set, langCode.get(lRef), LANGCODE);
			}

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
				if ( new File(getRepoRoot(), localDir).exists() ) {
					downloaded.add(fAtts);
				}

				sb = XbrlUtils.sbAppend(null, IDSEP, true, XBRL_ENTITYID_LEI, eid, date, XBRL_SOURCE_FILINGS, extra);
				String internalId = sb.toString();
				XbrlUtils.access(fAtts, AccessCmd.Set, internalId, REPORT_ID);

				getReportData().put(internalId, fAtts);
			}
		}

		System.out.println("Returned count: " + count + ", size of filings: " + filings.size() + ", local report count: " + getReportData().size() + ", downloaded: " + downloaded.size());
	}

	public DustUtilsData.TableReader getTableReader(String repId) throws Exception {
		optLoadFacts(repId);
		return headers.get(repId);
	}

	public Iterable<String[]> getFacts(String repId) throws Exception {
		return optLoadFacts(repId) ? (null == allFactsByRep) ? new FactIter(repId) : allFactsByRep.get(repId) : null;
	}

	public boolean optLoadFacts(String repId) throws Exception {
		if ( headers.containsKey(repId) ) {
			return true;
		}

		File f = getFactFile(repId);

		if ( (null != f) && f.isFile() ) {
			try (BufferedReader br = new BufferedReader(new FileReader(f))) {
				DustUtilsData.TableReader tr = null;
				ArrayList<String[]> allFacts = null;

				for (String line; (line = br.readLine()) != null;) {
					if ( !DustUtils.isEmpty(line) ) {
						String[] data = line.split("\t");

						if ( null == tr ) {
							tr = contentReaders.get(line);
							if ( null == tr ) {
								tr = new DustUtilsData.TableReader(data);
								contentReaders.put(line, tr);
							}
							headers.put(repId, tr);

							if ( null == allFactsByRep ) {
								return true;
							} else {
								allFacts = new ArrayList<>();
								allFactsByRep.put(repId, allFacts);
							}
						} else {
							allFacts.add(data);
						}
					}
				}
				return true;
			}
		}

		return false;
	}

	public File getFactFile(String repId) {
		Map mapFiling = getReportData().get(repId);
		String lDir = Dust.access(mapFiling, MindAccess.Peek, null, XbrlFilingManager.LOCAL_DIR);
		String fId = XbrlUtils.access(mapFiling, AccessCmd.Peek, null, "fxo_id");
		String fileName = lDir + "/" + fId + XbrlReportLoaderDomBase.POSTFIX_VAL;
		File f = new File(repoRoot, fileName);
		return f;
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

	public ArrayList<Map> getReports(Map<String, Object> match, ArrayList<Map> target) {
		if ( null == target ) {
			target = new ArrayList<>();
		} else {
			target.clear();
		}

		for (Map atts : getReportData().values()) {
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
				target.add(atts);
			}
		}

		return target;
	}

	int segmentSize = 10;
	long tsStart = -1;
	long tsSeg = -1;

	long sizeAll = 0;
	long sizeSeg = 0;

	public File getReport(Map filing, XbrlReportType repType, boolean downloadMissing) throws Exception {
		File ret = null;

		String repDir = XbrlUtils.access(filing, AccessCmd.Peek, null, LOCAL_DIR);
		File dir = new File(getRepoRoot(), repDir);
		dir.mkdirs();

		File fLoaded = null;
		String genFilePrefix = XbrlUtils.access(filing, AccessCmd.Peek, null, "fxo_id");
		String genFileName = null;

//		Map<String, String> ovr = urlOverride.getOrDefault(repType.name(), Collections.EMPTY_MAP);

		switch ( repType ) {
		case ContentVal:
			genFileName = genFilePrefix + XbrlReportLoaderDomBase.POSTFIX_VAL;
			repType = XbrlReportType.Zip;
			break;
		case ContentTxt:
			genFileName = genFilePrefix + XbrlReportLoaderDomBase.POSTFIX_TXT;
			repType = XbrlReportType.Zip;
			break;
		case GenJson:
			genFileName = genFilePrefix + "_Extract.json";
			repType = XbrlReportType.Json;
			break;
		default:
			genFileName = null;
			break;
		}

		boolean uaifrs = false;
		if ( null != genFileName ) {
			fLoaded = new File(dir, genFileName);
			uaifrs = genFileName.contains("UAIFRS");
			if ( fLoaded.isFile() && !forceReload ) 
//			if ( fLoaded.isFile() && !forceReload && !uaifrs ) 
			{
				return fLoaded;
			}
		}

		boolean directReport = false;
		String repUrl = XbrlUtils.access(filing, AccessCmd.Peek, null, (repType == XbrlReportType.Json) ? "json_url" : "package_url");
		if ( null == repUrl ) {
			// fallback if only report url is given
			repUrl = XbrlUtils.access(filing, AccessCmd.Peek, null, "report_url");
			if ( null == repUrl ) {
				return null;
			} else {
				directReport = true;
			}
		}

		String url = XBRL_ORG_ADDR + repUrl;
		int sep = url.lastIndexOf('/');
		String fName = url.substring(sep + 1);
		String retFileName = fName;

		ret = new File(dir, retFileName);

		if ( !ret.exists() ) {
			if ( !downloadMissing ) {
				return null;
			}
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

				if ( --downloadLimit < 0 ) {
					return null;
				}
				XbrlUtils.download(url, remoteFile);

				long s = remoteFile.length();
				sizeAll += s;
				sizeSeg += s;
				if ( 0 == (downloadLimit % segmentSize) ) {
					long ts = System.currentTimeMillis();
					System.out.println("Remaining downloads " + downloadLimit + ", segment speed: " + (sizeSeg / (ts - tsSeg)) + ", total speed: " + (sizeAll / (ts - tsStart)));

					tsSeg = -1;
					sizeSeg = 0;
				}
//				return null;
			} else {
				System.out.println("Resolving file from local cache " + fName);
			}

			if ( downloadOnly ) {
				return null;
			}

//			if ( repType == XbrlReportType.Package ) {
//				System.out.println("Unzipping " + remoteFile.getName() + " to: " + ret.getName());
//				try {
//					XbrlUtils.unzip(remoteFile, ret);
//				} catch (Throwable e) {
//					System.err.println("Unzip error with file " + remoteFile);
//					e.printStackTrace(System.err);
//				}
//
//			} else {
			ret = remoteFile;
//			}
		} else {
//			System.out.println("Resolving file from local cache " + fName);
		}

		if ( null != fLoaded ) {
			File fSrc = getReport(filing, repType, true);
			if ( (null != fSrc) && fSrc.isFile() ) {

				if ( repType == XbrlReportType.Json ) {
					Object report = parser.parse(new FileReader(fSrc));

					Map valFacts = (Map) XbrlUtils.access(report, AccessCmd.Peek, null, "facts");
					for (Object f : valFacts.values()) {
						String val = XbrlUtils.access(f, AccessCmd.Peek, "", "value");
						if ( val.length() > TEXT_CUT_AT ) {
							val = val.substring(0, TEXT_CUT_AT);
							val = val.replaceAll("\\s+", " ");
							XbrlUtils.access(f, AccessCmd.Set, val, "value");
						}
					}

					FileWriter fw = new FileWriter(fLoaded);
					JSONValue.writeJSONString(report, fw);
					fw.flush();
					fw.close();
				} else {
					File fRep = new File(dir, genFilePrefix + "_Report.xhtml");
					if ( !fRep.exists() ) {
						repUrl = XbrlUtils.access(filing, AccessCmd.Peek, null, "report_url");

						if ( !DustUtils.isEmpty(repUrl) ) {
							String pkgUrl = XbrlUtils.access(filing, AccessCmd.Peek, null, "package_url");

							if ( DustUtils.isEmpty(pkgUrl) ) {
								if ( directReport ) {
									System.out.println("Copy file " + ret + " to " + fRep);
//									ret.renameTo(fRep);
									Files.copy(ret.toPath(), fRep.toPath(), StandardCopyOption.REPLACE_EXISTING);
								}

							} else {
								sep = pkgUrl.lastIndexOf("/");
								String repName = repUrl.substring(sep + 1);

								XbrlTestPortalUtils.extractWithApacheZipFile(fSrc, fRep, repName);
							}
						}

						// repUrl extraction fails in some old cases, safety fallback
						if ( !fRep.isFile() ) {
							XbrlTestPortalUtils.extractWithApacheZipFile(fSrc, fRep, reportFilter);
						}
					}
					if ( (null != fRep) && fRep.isFile() ) {
						try {
							if ( uaifrs ) {
								filing.put(LANGFORCED, "uk");
							}							
							XbrlReportLoaderDomBase.createSplitCsv(fRep, dir, genFilePrefix, filing, TEXT_CUT_AT);
//							if ( !uaifrs ) 
							{
								fRep.delete();
							}
							ret = fLoaded;
						} catch (Throwable t) {
							DustException.swallow(t, "reading xhtml report", fRep.getCanonicalPath());
						}
					}
				}
			}
		}

		return ret;
	}

	public Map<String, Map> getReportData() {
		return reportData;
	}

	public File getRepoRoot() {
		return repoRoot;
	}

	public void loadAllData() throws Exception {
		System.out.println("Filing manager loadAllData...");

		String cg = System.getProperty("ClearGen", "false");
		boolean clearGen = "true".equals(cg);

		if ( clearGen ) {
			System.out.println("  + cleaning old generated files");
		}

		XbrlUtilsCounter dc = new XbrlUtilsCounter(true);

		String[] genFiles = { "extractedJson.json", "Report_Val.csv", "Report_Txt.csv" };

		int count = 0;

		for (Map.Entry<String, Map> e : reportData.entrySet()) {
			if ( 0 == (++count % 100) ) {
				System.out.println("Count " + count);
			}

			Map repSrc = e.getValue();

			if ( clearGen ) {
				String repDirName = XbrlUtils.access(repSrc, AccessCmd.Peek, null, LOCAL_DIR);
				File repDir = new File(getRepoRoot(), repDirName);

				for (String gf : genFiles) {
					File f = new File(repDir, gf);
					if ( f.isFile() ) {
						f.delete();
						dc.add("Deleted old gen file " + gf);
					}
				}
			}

			dc.add("Report seen");
			try {
				File f = getReport(repSrc, XbrlReportType.ContentVal, true);
				if ( (null != f) && f.isFile() ) {
					dc.add("Report loaded");
				} else {
					dc.add("Missing report from " + repSrc.get("country"));
					dc.add("Missing report date " + ((String) repSrc.get("date_added")).split(" ")[0]);
					System.out.println("Missing report " + repSrc);
				}
			} catch (Throwable err) {
				DustException.swallow(err);
			}
		}

		dc.dump("ESEF init summary");
	}

}
