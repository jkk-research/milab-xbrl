package com.xbrldock.poc.conn.xbrlorg;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevMonitor;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.poc.format.XbrlDockFormatUtils;
import com.xbrldock.poc.format.XbrlDockFormatXhtml;
import com.xbrldock.poc.utils.XbrlDockPocReportInfoExtender;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsJson;
import com.xbrldock.utils.XbrlDockUtilsNet;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockConnXbrlOrg implements XbrlDockConnXbrlOrgConsts, XbrlDockPocConsts.XDModSourceConnector {
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
				case XDC_FNAME_METAINF:
					metaInf = f;
					break;
				case XDC_FNAME_REPORTS:
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
	
	public XbrlDockConnXbrlOrg() {
		// TODO Auto-generated constructor stub
	}

	public XbrlDockConnXbrlOrg(String rootPath, String cachePath) throws Exception {
		dataRoot = new File(rootPath);
		cacheRoot = new File(cachePath);

		if (!dataRoot.exists()) {
			dataRoot.mkdirs();
		}
		
		testMode = true;

		catalog = XbrlDockUtilsJson.readJson(new File(dataRoot, PATH_CATALOG));

		if (null == catalog) {
			catalog = new TreeMap();
			refresh(null);
		} else {
			Map entities = XbrlDockUtils.safeGet(catalog, XDC_CONN_CAT_TOKEN_entities, MAP_CREATOR);
			Map langs = XbrlDockUtils.safeGet(catalog, XDC_CONN_CAT_TOKEN_languages, MAP_CREATOR);
			Map filings = XbrlDockUtils.safeGet(catalog, XDC_CONN_CAT_TOKEN_filings, MAP_CREATOR);
			XbrlDock.log(EventLevel.Trace, "Read filing count", filings.size(), "Entity count", entities.size(), "Language count", langs.size());
		}
	}
	
	@Override
	public void initModule(GenApp app, Map config) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public <RetType> RetType process(String command, Object... params) throws Exception {
		Object ret = null;
		
		switch (command) {
		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}
		
		return (RetType) ret;

	}

	@Override
	public Map getReportData(String id, Map target) throws Exception {
		Map filingData = XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings, id);

		if (null == filingData) {
			throw new Exception("Missing report id " + id);
		}

		if (null == target) {
			target = new HashMap();
		} else {
			target.clear();
		}
		
		target.putAll(filingData);

//		for (Map.Entry<Object, Object> me : ((Map<Object, Object>) filingData).entrySet()) {
//			target.put(me.getKey(), XbrlDockUtils.deepCopyIsh(me.getValue()));
//		}

		return target;
	}

	public void test() throws Exception {
//		XbrlDockConnXbrlOrgTest.test();
//		refresh();

	getFiling("529900SGCREUZCZ7P020-2024-06-30-ESEF-DK-0");


//		getAllFilings();
	}

	@Override
	public void visitReports(GenProcessor<Map> visitor, GenProcessor<Map> filter) throws Exception {
		XbrlDockDevMonitor pm = new XbrlDockDevMonitor("visitReports", 100);

		Map<String, Map<String, Object>> filings = XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings);

		visitor.process(null, ProcessorAction.Begin);

		for (Map.Entry<String, Map<String, Object>> fe : filings.entrySet()) {
			String k = fe.getKey();
			Map<String, Object> fd = fe.getValue();

			try {
				if ((null == filter) || filter.process(fd, ProcessorAction.Process)) {
					if (pm.step()) {
//						break;
					}
					boolean cont = visitor.process(fd, ProcessorAction.Process);

					if (!cont) {
						break;
					}
				}
			} catch (Throwable t) {
				XbrlDockException.swallow(t, "Filing key", k);
			}
		}

		visitor.process(null, ProcessorAction.End);
	}

	@Override
	public File getReportFile(String id, Object... keyPath) {
		File ret = null;

		Map filingData = XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings, id);

		if (null != filingData) {
			String localPath = (String) filingData.get( (null == keyPath) ? XDC_REPORT_TOKEN_localPath : keyPath );

			if (!XbrlDockUtils.isEmpty(localPath)) {
				ret = new File(cacheRoot, localPath);

				if (!ret.exists()) {
					ret = null;
				}
			}
		}

		return ret;
	}

	public void getAllFilings() throws Exception {
		XbrlDockDevMonitor pm = new XbrlDockDevMonitor("getAllFilings", 100);

		Map<String, Map<String, Object>> filings = XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings);

		int idx = 0;

		File logDir = new File("temp/log/");
		XbrlDockUtilsFile.ensureDir(logDir);
//		File log = new File(logDir, XbrlDockUtils.strTime() + XDC_FEXT_LOG);

//		XbrlDockTaxonomyRefCollector dh = new XbrlDockTaxonomyRefCollector();
		XbrlDockPocReportInfoExtender dh = new XbrlDockPocReportInfoExtender();
		ReportFormatHandler fh = new XbrlDockFormatXhtml();

//		long ts = System.currentTimeMillis();

//		try (PrintStream ps = new PrintStream(log)) 
		{
//			XbrlDock.setLogStream(ps);
			for (Map.Entry<String, Map<String, Object>> fe : filings.entrySet()) {
				String k = fe.getKey();
				try {
					if (pm.step()) {
//						long t = System.currentTimeMillis();
//						XbrlDock.handleLogDefault(System.out, EventLevel.Trace, "Process", idx, "segment time", (t - ts));
//						ts = t;

//						break;
					}
					File f = getFiling(k);

					if ((null != f) && f.isFile()) {
						++idx;
//						XbrlDock.log(EventLevel.Info, ++idx, k, f.getCanonicalPath());

						dh.setReportData(fe.getValue());
						loadReport(fh, dh, fe.getValue(), f);
					}
				} catch (Throwable t) {
					XbrlDockException.swallow(t, "Filing key", k);
				}
			}

//			XbrlDockUtilsJson.writeJson(new File(dataRoot, PATH_CATALOG), catalog);

			XbrlDock.log(EventLevel.Info, "Found filing count", idx);

			XbrlDock.log(EventLevel.Info, dh);
//		} finally {
//			XbrlDock.setLogStream(null);
		}
	}

	int missingMetaInf = 0;

	private void loadReport(ReportFormatHandler fh, ReportDataHandler dh, Map<String, Object> filingData, File f)
			throws IOException, Exception, FileNotFoundException {


		if (loadReport) {
			try (FileInputStream fr = new FileInputStream(f)) {
				dh.beginReport(f.getCanonicalPath());
//				dh.init( prefixes);				

				fh.loadReport(fr, dh);
				dh.endReport();
			}
		}
	}

	public File getFiling(String filingID) throws Exception {
		File ret = null;
		Map filingData = XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings, filingID);

		String path = (String) filingData.get( XDC_REPORT_TOKEN_localFilingPath);

		if (!XbrlDockUtils.isEmpty(path)) {
			ret = new File(cacheRoot, path);
			if (ret.isFile()) {
				path = (String) filingData.get( XDC_REPORT_TOKEN_localMetaInfPath);
				if (!XbrlDockUtils.isEmpty(path)) {
//					return ret;
				}
			}
		}

		String zipUrl = (String) filingData.get(XDC_REPORT_TOKEN_urlPackage);
		if (XbrlDockUtils.isEmpty(zipUrl)) {
			return null;
		}

		String str;

		str = (String) filingData.get(XDC_REPORT_TOKEN_entityId);
		String[] eid = str.split(XDC_SEP_ID);
		path = XbrlDockUtils.getHash2(eid[1], File.separator);
		path = XbrlDockUtils.sbAppend(null, File.separator, false, PATH_FILING_CACHE, eid[0], path, eid[1], filingID).toString();

		filingData.put(XDC_REPORT_TOKEN_localPath, path);
		
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

		str = XbrlDockUtils.simpleGet(filingData, XDC_REPORT_TOKEN_sourceAtts, XDC_XBRLORG_TOKEN_json_url);
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

		String packStatus = XDC_CONN_PACKAGE_PROC_MSG_reportNotFound;
		str = XbrlDockUtils.simpleGet(filingData, XDC_REPORT_TOKEN_sourceAtts, XDC_XBRLORG_TOKEN_report_url);

		rf.process(null, ProcessorAction.Init);
		XbrlDockUtilsFile.processFiles(fDir, rf, null, true, false);

		if (!storeRelativePath(filingData, XDC_REPORT_TOKEN_localMetaInfPath, rf.metaInf)) {
			XbrlDock.log(EventLevel.Error, "META_INF not found", filingID, fDir.getCanonicalPath());
		}

		if (!XbrlDockUtils.isEmpty(str)) {
			String prefix = XbrlDockUtils.cutPostfix(zipUrl, "/");
			String filingFileName = str.substring(prefix.length() + 1);
			ret = new File(fZipDir, filingFileName);
			packStatus = null;
		}

		if ((null == ret) || !ret.isFile()) {
			if (null != rf.reports) {
				File[] rc = rf.reports.listFiles(filingCandidate);

				if (0 < rc.length) {
					ret = rc[0];
					packStatus = (1 == rc.length) ? XDC_CONN_PACKAGE_PROC_MSG_reportFoundSingle : XDC_CONN_PACKAGE_PROC_MSG_reportFoundMulti;
				} else {
					repColl.process(null, ProcessorAction.Init);
					XbrlDockUtilsFile.processFiles(rf.reports, repColl, filingCandidate);
					Iterator<File> it = repColl.getFound();
					if (it.hasNext()) {
						packStatus = XDC_CONN_PACKAGE_PROC_MSG_reportMisplaced;
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
					packStatus = XDC_CONN_PACKAGE_PROC_MSG_reportMisplaced;
				}
			}
		}

		if (packStatus == XDC_CONN_PACKAGE_PROC_MSG_reportNotFound) {
			XbrlDock.log(EventLevel.Error, "Filing not found", filingID, fZipDir.getCanonicalPath(), str);
		} else {
			storeRelativePath(filingData, XDC_REPORT_TOKEN_localFilingPath, ret);
		}

		return ret;
	}

	private boolean storeRelativePath(Map filingData, Object key, File file) throws IOException {
		if (null != file) {
			String path = file.getCanonicalPath().substring(cacheRoot.getCanonicalPath().length() + 1);
			filingData.put(key,  path);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int refresh(Collection<String> updated) throws Exception {
		int newCount = 0;

		if (null != updated) {
			updated.clear();
		}

//		Map resp = XbrlDockUtilsJson.readJson(new File(dataRoot, PATH_SRVRESP));
		Map resp = XbrlDockUtilsJson.readJson(new File("temp/ESEFTest/20240907_ESEF_All.json"));

		Map entities = XbrlDockUtils.safeGet(catalog, XDC_CONN_CAT_TOKEN_entities, MAP_CREATOR);
		Map langs = XbrlDockUtils.safeGet(catalog, XDC_CONN_CAT_TOKEN_languages, MAP_CREATOR);

		Map atts;
		String id;
		Map<String, String> locLang = new HashMap<>();
		Map<String, Object> locEnt = new TreeMap<>();
		Map entityData;

		Collection<Map<String, Object>> c;

		c = XbrlDockUtils.simpleGet(resp, XDC_JSONAPI_TOKEN_included);
		for (Map<String, Object> i : c) {
			id = XbrlDockUtils.simpleGet(i, XDC_JSONAPI_TOKEN_id);
			atts = XbrlDockUtils.simpleGet(i, XDC_JSONAPI_TOKEN_attributes);
			String rt = (String) i.get(XDC_JSONAPI_TOKEN_type);

			switch (rt) {
			case XDC_XBRLORG_TOKEN_entity:
				String eid = XDC_ENTITY_ID_TYPE_LEI + XDC_SEP_ID + XbrlDockUtils.simpleGet(atts, XDC_XBRLORG_TOKEN_identifier);
				entityData = XbrlDockUtils.safeGet(entities, eid, MAP_CREATOR);
				
				entityData.put(XDC_ENTITY_TOKEN_id, eid);
				entityData.put(XDC_ENTITY_TOKEN_name, atts.get( XDC_XBRLORG_TOKEN_name));
				entityData.put(XDC_ENTITY_TOKEN_urlSource, XbrlDockUtils.simpleGet(i, XDC_JSONAPI_TOKEN_links, XDC_JSONAPI_TOKEN_self));

				locEnt.put(id, entityData);

				break;
			case XDC_XBRLORG_TOKEN_language:
				String code = XbrlDockUtils.simpleGet(atts, XDC_XBRLORG_TOKEN_code);
				langs.put(code, XbrlDockUtils.simpleGet(atts, XDC_XBRLORG_TOKEN_name));
				locLang.put(id, code);
				break;
			default:
				XbrlDockException.wrap(null, "Should not be here");
				break;
			}
		}

		Map filings = XbrlDockUtils.safeGet(catalog, XDC_CONN_CAT_TOKEN_filings, MAP_CREATOR);

		c = XbrlDockUtils.simpleGet(resp, XDC_JSONAPI_TOKEN_data);
		XbrlDock.log(EventLevel.Trace, "Data count", c.size());
		for (Map<String, Object> i : c) {
			atts = XbrlDockUtils.simpleGet(i, XDC_JSONAPI_TOKEN_attributes);
			String rt = (String) i.get(XDC_JSONAPI_TOKEN_type);

			switch (rt) {
			case XDC_XBRLORG_TOKEN_filing:
				id = XbrlDockUtils.simpleGet(atts, XDC_XBRLORG_TOKEN_fxo_id);
				Map filingData = XbrlDockUtils.safeGet(filings, id, MAP_CREATOR);

				filingData.put(XDC_REPORT_TOKEN_source, sourceName);
				filingData.put(XDC_REPORT_TOKEN_id, id);
				filingData.put(XDC_REPORT_TOKEN_sourceAtts, atts);

				filingData.put(XDC_REPORT_TOKEN_periodEnd, atts.get(XDC_XBRLORG_TOKEN_period_end));
				filingData.put(XDC_REPORT_TOKEN_published, atts.get(XDC_XBRLORG_TOKEN_date_added));
				filingData.put(XDC_REPORT_TOKEN_urlPackage, atts.get(XDC_XBRLORG_TOKEN_package_url));
				filingData.put(XDC_REPORT_TOKEN_sourceUrl, XbrlDockUtils.simpleGet(i, XDC_JSONAPI_TOKEN_links, XDC_JSONAPI_TOKEN_self));

				String linkId = XbrlDockUtils.simpleGet(i, XDC_JSONAPI_TOKEN_relationships, XDC_XBRLORG_TOKEN_language, XDC_JSONAPI_TOKEN_data, XDC_JSONAPI_TOKEN_id);
				filingData.put(XDC_REPORT_TOKEN_langCode, locLang.get(linkId));

				linkId = XbrlDockUtils.simpleGet(i, XDC_JSONAPI_TOKEN_relationships, XDC_XBRLORG_TOKEN_entity, XDC_JSONAPI_TOKEN_data, XDC_JSONAPI_TOKEN_id);
				entityData = (Map) locEnt.get(linkId);
				filingData.put(XDC_REPORT_TOKEN_entityId, entityData.get(XDC_ENTITY_TOKEN_id));
				filingData.put(XDC_REPORT_TOKEN_entityName, entityData.get(XDC_ENTITY_TOKEN_name));

				++newCount;
				if (null != updated) {
					updated.add(id);
				}

				break;
			default:
				XbrlDockException.wrap(null, "Should not be here");
				break;
			}
		}

		XbrlDock.log(EventLevel.Trace, "Read filing count", filings.size(), "Entity count", entities.size(), "Language count", langs.size());

//		XbrlDockUtilsJson.writeJson(new File(dataRoot, PATH_CATALOG), catalog);

		return (null == updated) ? newCount : updated.size();
	}
}
