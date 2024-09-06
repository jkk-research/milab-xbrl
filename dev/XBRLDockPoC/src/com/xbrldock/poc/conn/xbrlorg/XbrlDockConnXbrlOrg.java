package com.xbrldock.poc.conn.xbrlorg;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsJson;
import com.xbrldock.utils.XbrlDockUtilsNet;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockConnXbrlOrg implements XbrlDockConnXbrlOrgConsts {
	String sourceName = "xbrl.org";

	String urlRoot = "https://filings.xbrl.org/";

	File dataRoot;
	File cacheRoot;

	Map catalog;

	public XbrlDockConnXbrlOrg(String rootPath, String cachePath) throws Exception {
		dataRoot = new File(rootPath);
		cacheRoot = new File(cachePath);

		if (!dataRoot.exists()) {
			dataRoot.mkdirs();
		}

		catalog = XbrlDockUtilsJson.readJson(new File(dataRoot, PATH_CATALOG));

		if (null == catalog) {
			catalog = new TreeMap();
		}
	}

	public void test() throws Exception {
//		XbrlDockConnXbrlOrgTest.test();
		refresh();
		
		Map<String, Object> filings = XbrlDockUtils.simpleGet(catalog, CatalogKeys.filings);
		
		for ( String k : filings.keySet() ) {
			try {
				File f = getFiling(k);
				
				XbrlDock.log(EventLevel.Info, k, (null == f) ? "missing" : f.getCanonicalPath());

			} catch ( Throwable t ) {
				XbrlDockException.swallow(t, "Filing key", k);
			}
		}
		
//		getFiling("529900SGCREUZCZ7P020-2024-06-30-ESEF-DK-0");
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

		path = XbrlDockUtils.sbAppend(null, File.separator, false, PATH_FILING_CACHE, eid[0], path, eid[1], filingID)
				.toString();

		File fDir = new File(cacheRoot, path);
		XbrlDockUtilsFile.ensureDir(fDir);

		String zipFile = XbrlDockUtils.getPostfix(zipUrl, "/");

		String zipDir = XbrlDockUtils.cutPostfix(zipFile, ".");
		File fZipDir = new File(fDir, zipDir);
		
		File fZip = new File(fDir, zipFile);

		if (!fZip.isFile()) {
			XbrlDock.log(EventLevel.Trace, "Downloading filing package", fZip.getCanonicalPath());
			XbrlDockUtilsNet.download(urlRoot + zipUrl, fZip);
		}

		if ( !fZipDir.isDirectory() && fZip.isFile()) {
			XbrlDock.log(EventLevel.Trace, "Unzipping package", fZip.getCanonicalPath());
			XbrlDockUtilsFile.extractWithApacheZipFile(fZipDir, fZip, null);
		}
		
		str = XbrlDockUtils.simpleGet(filingData, FilingKeys.sourceAtts, ResponseKeys.report_url);
		String prefix = XbrlDockUtils.cutPostfix(zipUrl, "/");
		
		String fileName = str.substring(prefix.length() + 1);
		
		ret = new File(fZipDir, fileName);
		
		if ( !ret.isFile()) {
			XbrlDockException.wrap(null, "Missing filing file", ret.getCanonicalPath());
		}
				
		return ret;
	}

	public void refresh() throws Exception {
		Map resp = XbrlDockUtilsJson.readJson(new File(dataRoot, PATH_SRVRESP));

		Map entities = XbrlDockUtils.safeGet(catalog, CatalogKeys.entities, MAP_CREATOR);
		Map langs = XbrlDockUtils.safeGet(catalog, CatalogKeys.languages, MAP_CREATOR);

		Map atts;
		String id;
		Map<String, String> locLang = new TreeMap<>();
		Map<String, Map> locEnt = new TreeMap<>();
		Map<EntityKeys, Object> entityData;

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
				entityData.put(EntityKeys.id, eid);
				entityData.put(EntityKeys.name, XbrlDockUtils.simpleGet(atts, ResponseKeys.name));
				entityData.put(EntityKeys.urlSource, XbrlDockUtils.simpleGet(i, JsonApiKeys.links, JsonApiKeys.self));
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
		for (Map<String, Object> i : c) {
			atts = XbrlDockUtils.simpleGet(i, JsonApiKeys.attributes);
			ResponseType rt = XbrlDockUtils.simpleGetEnum(ResponseType.class, i, JsonApiKeys.type);

			switch (rt) {
			case filing:
				id = XbrlDockUtils.simpleGet(atts, ResponseKeys.fxo_id);
				Map<FilingKeys, Object> filingData = XbrlDockUtils.safeGet(filings, id, MAP_CREATOR);

				filingData.put(FilingKeys.source, sourceName);
				filingData.put(FilingKeys.id, id);
				filingData.put(FilingKeys.periodEnd, XbrlDockUtils.simpleGet(atts, ResponseKeys.period_end));
				filingData.put(FilingKeys.published, XbrlDockUtils.simpleGet(atts, ResponseKeys.date_added));
				filingData.put(FilingKeys.urlPackage, XbrlDockUtils.simpleGet(atts, ResponseKeys.package_url));
				filingData.put(FilingKeys.sourceUrl, XbrlDockUtils.simpleGet(i, JsonApiKeys.links, JsonApiKeys.self));
				filingData.put(FilingKeys.sourceAtts, atts);

				String linkId = XbrlDockUtils.simpleGet(i, JsonApiKeys.relationships, ResponseType.language, JsonApiKeys.data,
						JsonApiKeys.id);
				filingData.put(FilingKeys.langCode, locLang.get(linkId));

				linkId = XbrlDockUtils.simpleGet(i, JsonApiKeys.relationships, ResponseType.entity, JsonApiKeys.data,
						JsonApiKeys.id);
				entityData = locEnt.get(linkId);
				filingData.put(FilingKeys.entityId, entityData.get(EntityKeys.id));
				filingData.put(FilingKeys.entityName, entityData.get(EntityKeys.name));

				break;
			default:
				XbrlDockException.wrap(null, "Should not be here");
				break;
			}
		}

		XbrlDockUtilsJson.writeJson(new File(dataRoot, PATH_CATALOG), catalog);
	}
}
