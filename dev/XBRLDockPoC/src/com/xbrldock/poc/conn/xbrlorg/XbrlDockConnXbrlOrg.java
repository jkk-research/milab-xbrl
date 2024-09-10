package com.xbrldock.poc.conn.xbrlorg;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.XbrlDockPoc;
import com.xbrldock.poc.format.XbrlDockFormatUtils;
import com.xbrldock.poc.format.XbrlDockFormatXhtml;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsDumpReportHandler;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsJson;
import com.xbrldock.utils.XbrlDockUtilsMonitor;
import com.xbrldock.utils.XbrlDockUtilsNet;
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockConnXbrlOrg implements XbrlDockConnXbrlOrgConsts {
	String sourceName = "xbrl.org";

	String urlRoot = "https://filings.xbrl.org/";

	File dataRoot;
	File cacheRoot;

	Map catalog;

	class DirMapper implements XbrlDockUtilsFile.FileProcessor {
		File metaInf;
		File reports;

		@Override
		public boolean process(File f, ProcessorAction action) {
			switch (action) {
			case Init:
				reports = metaInf = null;
				break;
			case Begin:
				switch (f.getName()) {
				case XBRLDOCK_FNAME_METAINF:
					metaInf = f;
					break;
				case XBRLDOCK_FNAME_REPORTS:
					reports = f;
					break;
				}
				break;
			default:
				break;
			}

			return true;
		}
	};

	DirMapper rf = new DirMapper();

	FileFilter filingCandidate = new FileFilter() {
		@Override
		public boolean accept(File f) {
			return XbrlDockFormatUtils.canBeXbrl(f);
		}
	};

	XbrlDockUtilsFile.FileCollector repColl = new XbrlDockUtilsFile.FileCollector();

	public boolean testMode;
//	public boolean loadReport;
	public boolean loadReport = true;

	public XbrlDockConnXbrlOrg(String rootPath, String cachePath) throws Exception {
		dataRoot = new File(rootPath);
		cacheRoot = new File(cachePath);

		if (!dataRoot.exists()) {
			dataRoot.mkdirs();
		}

		catalog = XbrlDockUtilsJson.readJson(new File(dataRoot, PATH_CATALOG));

		if (null == catalog) {
			catalog = new TreeMap();
			refresh();
		} else {
			Map entities = XbrlDockUtils.safeGet(catalog, CatalogKeys.entities, MAP_CREATOR);
			Map langs = XbrlDockUtils.safeGet(catalog, CatalogKeys.languages, MAP_CREATOR);
			Map filings = XbrlDockUtils.safeGet(catalog, CatalogKeys.filings, MAP_CREATOR);
			XbrlDock.log(EventLevel.Trace, "Read filing count", filings.size(), "Entity count", entities.size(), "Language count", langs.size());
		}
	}

	public void test() throws Exception {
//		XbrlDockConnXbrlOrgTest.test();
//		refresh();

//	getFiling("529900SGCREUZCZ7P020-2024-06-30-ESEF-DK-0");

//		testMode = true;

		getAllFilings();
	}

	public void getAllFilings() throws Exception {
		XbrlDockUtilsMonitor pm = new XbrlDockUtilsMonitor("getAllFilings", 100);

		Map<String, Map<String, Object>> filings = XbrlDockUtils.simpleGet(catalog, CatalogKeys.filings);

		int idx = 0;

		File logDir = new File("temp/log/");
		XbrlDockUtilsFile.ensureDir(logDir);
		File log = new File(logDir, XbrlDockUtils.strTime() + XBRLDOCK_EXT_LOG);

		XbrlDockUtilsDumpReportHandler dh = new XbrlDockUtilsDumpReportHandler();
		dh.logAll = false;
		ReportFormatHandler fh = new XbrlDockFormatXhtml();

		long ts = System.currentTimeMillis();

		try (PrintStream ps = new PrintStream(log)) {
			XbrlDock.setLogStream(ps);
			for (Map.Entry<String, Map<String, Object>> fe : filings.entrySet()) {
				String k = fe.getKey();
				try {
					if (pm.step()) {
						long t = System.currentTimeMillis();
						XbrlDock.handleLogDefault(System.out, EventLevel.Trace, "Process", idx, "segment time", (t - ts));
						ts = t;

//						break;
					}
					File f = getFiling(k);

					if ((null != f) && f.isFile()) {
						++idx;
//						XbrlDock.log(EventLevel.Info, ++idx, k, f.getCanonicalPath());

						loadReport(fh, dh, fe.getValue(), f);
					}
				} catch (Throwable t) {
					XbrlDockException.swallow(t, "Filing key", k);
				}
			}

			XbrlDockUtilsJson.writeJson(new File(dataRoot, PATH_CATALOG), catalog);

			XbrlDock.log(EventLevel.Info, "Found filing count", idx);
		} finally {
			XbrlDock.setLogStream(null);
		}
	}

	int missingMetaInf = 0;

	private void loadReport(ReportFormatHandler fh, ReportDataHandler dh, Map<String, Object> filingData, File f)
			throws IOException, Exception, FileNotFoundException {

		String str = XbrlDockUtils.simpleGet(filingData, FilingKeys.localMetaInfPath);

		if (XbrlDockUtils.isEmpty(str)) {
			XbrlDock.log(EventLevel.Error, "Missing META_INF folder", f.getCanonicalPath());
			return;
		}
		File fMetaInf = new File(cacheRoot, str);

		if (!fMetaInf.isDirectory()) {
			XbrlDock.log(EventLevel.Error, "Missing META_INF folder", fMetaInf.getCanonicalPath());
			return;
		}

		NodeList nl;
		int nc;

		File fCat = new File(fMetaInf, XBRLDOCK_FNAME_CATALOG);
		Element eCatalog = XbrlDockUtilsXml.parseDoc(fCat, XbrlDockPoc.URL_CACHE).getDocumentElement();
		
		Map<String, File> prefixes = new TreeMap<>();

		nl = eCatalog.getElementsByTagName("*");
		nc = nl.getLength();

		for (int idx = 0; idx < nc; ++idx) {
			Element e = (Element) nl.item(idx);
			String tagName = e.getTagName();

			switch (tagName) {
			case "rewriteURI":
				prefixes.put(e.getAttribute("uriStartString"), new File(fMetaInf, e.getAttribute("rewritePrefix")));
				break;
			}
		}

		File fTax = new File(fMetaInf, XBRLDOCK_FNAME_TAXPACK);
		Element eTaxPack = XbrlDockUtilsXml.parseDoc(fTax, XbrlDockPoc.URL_CACHE).getDocumentElement();

		nl = eTaxPack.getElementsByTagName("*");
		nc = nl.getLength();

		for (int idx = 0; idx < nc; ++idx) {
			Element e = (Element) nl.item(idx);
			String tagName = e.getTagName();

			switch (tagName) {
			case "tp:entryPointDocument":
				str = e.getAttribute("href");
				
				for ( Map.Entry<String, File> pe : prefixes.entrySet()) {
					String p = pe.getKey();
					
					if ( str.startsWith(p) ) {
						File fr = new File(pe.getValue(), str.substring(p.length()));
						XbrlDockUtilsXml.parseDoc(fr, XbrlDockPoc.URL_CACHE);						
					}
				}

				break;
			}
		}

		if (loadReport) {
			try (FileInputStream fr = new FileInputStream(f)) {
				dh.beginReport(f.getCanonicalPath());
				fh.loadReport(fr, dh);
				dh.endReport();
			}
		}
	}

	public File getFiling(String filingID) throws Exception {
		File ret = null;
		Map filingData = XbrlDockUtils.simpleGet(catalog, CatalogKeys.filings, filingID);

		String path = XbrlDockUtils.simpleGet(filingData, FilingKeys.localFilingPath);

		if (!XbrlDockUtils.isEmpty(path)) {
			ret = new File(cacheRoot, path);
			if (ret.isFile()) {
				path = XbrlDockUtils.simpleGet(filingData, FilingKeys.localMetaInfPath);
				if (!XbrlDockUtils.isEmpty(path)) {
					return ret;
				}
			}
		}

		String zipUrl = XbrlDockUtils.simpleGet(filingData, FilingKeys.urlPackage);
		if (XbrlDockUtils.isEmpty(zipUrl)) {
			return null;
		}

		String str;

		str = XbrlDockUtils.simpleGet(filingData, FilingKeys.entityId);
		String[] eid = str.split(XBRLDOCK_SEP_ID);
		path = XbrlDockUtils.getHash2(eid[1], File.separator);
		path = XbrlDockUtils.sbAppend(null, File.separator, false, PATH_FILING_CACHE, eid[0], path, eid[1], filingID).toString();

		File fDir = new File(cacheRoot, path);
		XbrlDockUtilsFile.ensureDir(fDir);

		String zipFile = XbrlDockUtils.getPostfix(zipUrl, "/");
		String zipDir = XbrlDockUtils.cutPostfix(zipFile, ".");
		File fZipDir = new File(fDir, zipDir);
		File fZip = new File(fDir, zipFile);

		if (!fZip.isFile()) {
			XbrlDock.log(EventLevel.Trace, "Downloading filing package", fZip.getCanonicalPath());

			if (!testMode) {
				XbrlDockUtilsNet.download(urlRoot + zipUrl.replace(" ", "%20"), fZip);
			}
		}

		if (!fZipDir.isDirectory() && fZip.isFile()) {
			XbrlDock.log(EventLevel.Trace, "Unzipping package", fZip.getCanonicalPath());
			try {
				XbrlDockUtilsFile.extractWithApacheZipFile(fZipDir, fZip, null);
			} catch (Throwable t) {
				fZip.delete();
				throw t;
			}
		}

		str = XbrlDockUtils.simpleGet(filingData, FilingKeys.sourceAtts, ResponseKeys.json_url);
		if (!XbrlDockUtils.isEmpty(str)) {
			String prefix = XbrlDockUtils.cutPostfix(zipUrl, "/");
			String jsonFileName = str.substring(prefix.length() + 1);
			File fJson = new File(fDir, jsonFileName);

			if (!fJson.isFile()) {
				if (!testMode) {
					XbrlDock.log(EventLevel.Trace, "Downloading JSON", fJson.getCanonicalPath());
					XbrlDockUtilsNet.download(urlRoot + str.replace(" ", "%20"), fJson);
				}
//			} else {
//				XbrlDock.log(EventLevel.Trace, "Json file found", fJson.getCanonicalPath());				
			}
		}

		PackageStatus packStatus = PackageStatus.reportNotFound;
		str = XbrlDockUtils.simpleGet(filingData, FilingKeys.sourceAtts, ResponseKeys.report_url);

		rf.process(null, ProcessorAction.Init);
		XbrlDockUtilsFile.processFiles(fDir, rf, null, true, false);

		if (!storeRelativePath(filingData, FilingKeys.localMetaInfPath, rf.metaInf)) {
			XbrlDock.log(EventLevel.Error, "META_INF not found", filingID, fDir.getCanonicalPath());
		}

		if (!XbrlDockUtils.isEmpty(str)) {
			String prefix = XbrlDockUtils.cutPostfix(zipUrl, "/");
			String filingFileName = str.substring(prefix.length() + 1);
			ret = new File(fZipDir, filingFileName);
			packStatus = PackageStatus.reportIdentified;
		}

		if ((null == ret) || !ret.isFile()) {
			if (null != rf.reports) {
				File[] rc = rf.reports.listFiles(filingCandidate);

				if (0 < rc.length) {
					ret = rc[0];
					packStatus = (1 == rc.length) ? PackageStatus.reportFoundSingle : PackageStatus.reportFoundMulti;
				} else {
					repColl.process(null, ProcessorAction.Init);
					XbrlDockUtilsFile.processFiles(rf.reports, repColl, filingCandidate);
					Iterator<File> it = repColl.getFound();
					if (it.hasNext()) {
						packStatus = PackageStatus.reportMisplaced;
						ret = it.next();
					}
				}
			}
		}

		if ((null == ret) || !ret.isFile()) {
			if (null != rf.metaInf) {
				File[] rc = rf.metaInf.getParentFile().listFiles(filingCandidate);

				if (0 < rc.length) {
					ret = rc[0];
					packStatus = PackageStatus.reportMisplaced;
				}
			}
		}

		if (packStatus == PackageStatus.reportNotFound) {
			XbrlDock.log(EventLevel.Error, "Filing not found", filingID, fZipDir.getCanonicalPath(), str);
		} else {
			storeRelativePath(filingData, FilingKeys.localFilingPath, ret);
		}

		return ret;
	}

	private boolean storeRelativePath(Map filingData, Object key, File file) throws IOException {
		if (null != file) {
			String path = file.getCanonicalPath().substring(cacheRoot.getCanonicalPath().length() + 1);
			XbrlDockUtils.simpleSet(filingData, path, key);
			return true;
		} else {
			return false;
		}
	}

	public void refresh() throws Exception {
//		Map resp = XbrlDockUtilsJson.readJson(new File(dataRoot, PATH_SRVRESP));
		Map resp = XbrlDockUtilsJson.readJson(new File("temp/ESEFTest/20240907_ESEF_All.json"));

		Map entities = XbrlDockUtils.safeGet(catalog, CatalogKeys.entities, MAP_CREATOR);
		Map langs = XbrlDockUtils.safeGet(catalog, CatalogKeys.languages, MAP_CREATOR);

		Map atts;
		String id;
		Map<String, String> locLang = new HashMap<>();
		Map<String, Object> locEnt = new TreeMap<>();
		Object entityData;

		Collection<Map<String, Object>> c;

		c = XbrlDockUtils.simpleGet(resp, JsonApiKeys.included);
		for (Map<String, Object> i : c) {
			id = XbrlDockUtils.simpleGet(i, JsonApiKeys.id);
			atts = XbrlDockUtils.simpleGet(i, JsonApiKeys.attributes);
			ResponseType rt = XbrlDockUtils.simpleGetEnum(ResponseType.class, i, JsonApiKeys.type);

			switch (rt) {
			case entity:
				String eid = EntityIdType.lei + XBRLDOCK_SEP_ID + XbrlDockUtils.simpleGet(atts, ResponseKeys.identifier);
				entityData = XbrlDockUtils.safeGet(entities, eid, MAP_CREATOR);

				XbrlDockUtils.simpleSet(entityData, eid, EntityKeys.id);
				XbrlDockUtils.simpleSet(entityData, XbrlDockUtils.simpleGet(atts, ResponseKeys.name), EntityKeys.name);
				XbrlDockUtils.simpleSet(entityData, XbrlDockUtils.simpleGet(i, JsonApiKeys.links, JsonApiKeys.self), EntityKeys.urlSource);
				locEnt.put(id, entityData);

				break;
			case language:
				String code = XbrlDockUtils.simpleGet(atts, ResponseKeys.code);
				langs.put(code, XbrlDockUtils.simpleGet(atts, ResponseKeys.name));
				locLang.put(id, code);
				break;
			default:
				XbrlDockException.wrap(null, "Should not be here");
				break;
			}
		}

		Map filings = XbrlDockUtils.safeGet(catalog, CatalogKeys.filings, MAP_CREATOR);

		c = XbrlDockUtils.simpleGet(resp, JsonApiKeys.data);
		XbrlDock.log(EventLevel.Trace, "Data count", c.size());
		for (Map<String, Object> i : c) {
			atts = XbrlDockUtils.simpleGet(i, JsonApiKeys.attributes);
			ResponseType rt = XbrlDockUtils.simpleGetEnum(ResponseType.class, i, JsonApiKeys.type);

			switch (rt) {
			case filing:
				id = XbrlDockUtils.simpleGet(atts, ResponseKeys.fxo_id);
				Object filingData = XbrlDockUtils.safeGet(filings, id, MAP_CREATOR);

				XbrlDockUtils.simpleSet(filingData, sourceName, FilingKeys.source);
				XbrlDockUtils.simpleSet(filingData, id, FilingKeys.id);
				XbrlDockUtils.simpleSet(filingData, atts, FilingKeys.sourceAtts);

				XbrlDockUtils.simpleSet(filingData, XbrlDockUtils.simpleGet(atts, ResponseKeys.period_end), FilingKeys.periodEnd);
				XbrlDockUtils.simpleSet(filingData, XbrlDockUtils.simpleGet(atts, ResponseKeys.date_added), FilingKeys.published);
				XbrlDockUtils.simpleSet(filingData, XbrlDockUtils.simpleGet(atts, ResponseKeys.package_url), FilingKeys.urlPackage);
				XbrlDockUtils.simpleSet(filingData, XbrlDockUtils.simpleGet(i, JsonApiKeys.links, JsonApiKeys.self), FilingKeys.sourceUrl);

				String linkId = XbrlDockUtils.simpleGet(i, JsonApiKeys.relationships, ResponseType.language, JsonApiKeys.data, JsonApiKeys.id);
				XbrlDockUtils.simpleSet(filingData, locLang.get(linkId), FilingKeys.langCode);

				linkId = XbrlDockUtils.simpleGet(i, JsonApiKeys.relationships, ResponseType.entity, JsonApiKeys.data, JsonApiKeys.id);
				entityData = locEnt.get(linkId);
				XbrlDockUtils.simpleSet(filingData, XbrlDockUtils.simpleGet(entityData, EntityKeys.id), FilingKeys.entityId);
				XbrlDockUtils.simpleSet(filingData, XbrlDockUtils.simpleGet(entityData, EntityKeys.name), FilingKeys.entityName);

				break;
			default:
				XbrlDockException.wrap(null, "Should not be here");
				break;
			}
		}

		XbrlDock.log(EventLevel.Trace, "Read filing count", filings.size(), "Entity count", entities.size(), "Language count", langs.size());

		XbrlDockUtilsJson.writeJson(new File(dataRoot, PATH_CATALOG), catalog);
	}
}
