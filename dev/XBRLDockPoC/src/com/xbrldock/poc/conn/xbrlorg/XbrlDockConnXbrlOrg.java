package com.xbrldock.poc.conn.xbrlorg;

import java.io.File;
import java.io.FileFilter;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.format.XbrlDockFormatUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsJson;
import com.xbrldock.utils.XbrlDockUtilsMonitor;
import com.xbrldock.utils.XbrlDockUtilsNet;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockConnXbrlOrg implements XbrlDockConnXbrlOrgConsts {
	String sourceName = "xbrl.org";

	String urlRoot = "https://filings.xbrl.org/";

	File dataRoot;
	File cacheRoot;

	Map catalog;

	class RepFinder implements XbrlDockUtilsFile.FileProcessor {
		File result;

		void reset() {
			result = null;
		}

		@Override
		public boolean process(File f) {
			if (f.isDirectory() && XbrlDockUtils.isEqual(f.getName(), "reports")) {
				result = f;
				return false;
			} else {
				return null == result;
			}
		}
	};

	RepFinder rf = new RepFinder();

	FileFilter filingCandidate = new FileFilter() {
		@Override
		public boolean accept(File f) {
			return XbrlDockFormatUtils.canBeXbrl(f);
		}
	};

	public boolean testMode;

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

	@SuppressWarnings("unused")
	public void getAllFilings() throws Exception {
		XbrlDockUtilsMonitor pm = new XbrlDockUtilsMonitor("getAllFilings", 100);

		Map<String, Object> filings = XbrlDockUtils.simpleGet(catalog, CatalogKeys.filings);

		int idx = 0;

		File logDir = new File("temp/log/");
		XbrlDockUtilsFile.ensureDir(logDir);
		File log = new File(logDir, XbrlDockUtils.strTime() + XBRLDOCK_EXT_LOG);

		try (PrintStream ps = new PrintStream(log)) {
//			XbrlDock.setLogStream(ps);
			for (String k : filings.keySet()) {
				try {
//					pm.step();
					File f = getFiling(k);

					if ((null != f) && f.isFile()) {
//						XbrlDock.log(EventLevel.Info, ++idx, k, f.getCanonicalPath());
					}
				} catch (Throwable t) {
					XbrlDockException.swallow(t, "Filing key", k);
				}
			}
		} finally {
			XbrlDock.setLogStream(null);
		}
	}

	public File getFiling(String filingID) throws Exception {
		File ret = null;
		Map filingData = XbrlDockUtils.simpleGet(catalog, CatalogKeys.filings, filingID);

		String path = XbrlDockUtils.simpleGet(filingData, FilingKeys.localPath);

		if (!XbrlDockUtils.isEmpty(path)) {
			return new File(path);
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

//		filingFileName = "missing";
		PackageStatus packStatus;
		str = XbrlDockUtils.simpleGet(filingData, FilingKeys.sourceAtts, ResponseKeys.report_url);

		if (!XbrlDockUtils.isEmpty(str)) {
			String prefix = XbrlDockUtils.cutPostfix(zipUrl, "/");
			String filingFileName = str.substring(prefix.length() + 1);
			ret = new File(fZipDir, filingFileName);
		}

		if ((null == ret) || !ret.isFile()) {
			rf.reset();

			XbrlDockUtilsFile.processFiles(fDir, rf, null, true, false);

			if (null != rf.result) {
				File[] rc = rf.result.listFiles(filingCandidate);

				if (0 < rc.length) {
					ret = rc[0];
					packStatus = (1 == rc.length) ? PackageStatus.reportFoundSingle : PackageStatus.reportFoundMulti;
				} else {
					packStatus = PackageStatus.reportNotFound;
				}
			} else {
				packStatus = PackageStatus.reportMisplaced;
			}
		} else {
			packStatus = PackageStatus.reportIdentified;
		}
		
		if ((null == ret) || !ret.isFile()) {
			XbrlDock.log(EventLevel.Warning, "Missing filing file", fZipDir.getCanonicalPath(), str, packStatus);
		}

		return ret;
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
