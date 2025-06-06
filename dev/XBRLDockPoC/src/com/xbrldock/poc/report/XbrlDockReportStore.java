package com.xbrldock.poc.report;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevCounter;
import com.xbrldock.dev.XbrlDockDevMonitor;
import com.xbrldock.format.XbrlDockFormatAgentXmlWriter;
import com.xbrldock.format.XbrlDockFormatUtils;
import com.xbrldock.poc.XbrlDockPocRefactorUtils;
import com.xbrldock.poc.conn.XbrlDockConnUtils;
import com.xbrldock.poc.format.XbrlDockFormatAgentXhtmlReader;
import com.xbrldock.poc.utils.XbrlDockPocReportInfoExtender;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.stream.XbrlDockStreamJson;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockReportStore implements XbrlDockReportConsts, XbrlDockPocRefactorUtils, XbrlDockConsts.GenAgent {
	File dirStore;
	File dirInput;

	Map catalog;

	GenAgent agtConn;

	XbrlDockConnUtils.DirMapper rf = new XbrlDockConnUtils.DirMapper();

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

	public XbrlDockReportStore() {
		// TODO Auto-generated constructor stub
	}

	public void initModule(Map config) throws Exception {

		dirInput = XbrlDockUtilsFile.ensureDir((String) XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirInput));
		dirStore = XbrlDockUtilsFile.ensureDir((String) XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirStore));

		File fc = new File(dirStore, XDC_FNAME_CONNCATALOG);
		if (fc.isFile()) {
			catalog = XbrlDockStreamJson.readJson(fc);
		} else {
			catalog = new HashMap();
		}

		testMode = true;

		Map connInfo = XbrlDockUtils.simpleGet(config, XDC_STORE_CONNECTOR);

		if (null != connInfo) {
			agtConn = XbrlDockUtils.createObject(connInfo);
		}
	}

	@Override
	public Object process(String command, Map params) throws Exception {
		Object ret = null;
		Map<String, Map> filings = XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings);
		Map<String, Object> cp = new TreeMap<String, Object>();
		
		switch (command) {
		case XDC_CMD_GEN_Init:
			initModule(params);
			break;
		case XDC_CMD_GEN_GETCATALOG:
			ret = catalog;
			break;
		case XDC_CMD_GEN_TEST01:

			File fRoot = new File(dirInput, XDC_FNAME_CONNFILINGS);

			XbrlDockDevMonitor mon = new XbrlDockDevMonitor("Report count", 100);
			Set<String> msgs = new TreeSet<>();
			XbrlDockDevCounter cnt = new XbrlDockDevCounter("Visit errors", true);

			XbrlDockReportLoader dh = new XbrlDockReportLoader(dirStore);

			for (Map.Entry<String, Map> fe : filings.entrySet()) {
				mon.step();

				String id = fe.getKey();
				Map filingData = fe.getValue();

				File dataDir = XbrlDockConnUtils.getFilingDir(dirStore, filingData, true, false);

				if (new File(dataDir, XDC_FNAME_REPDATA).isFile()) {
//					continue;
				}

				File dir = XbrlDockConnUtils.getFilingDir(fRoot, filingData, true, false);
				if (dir.isDirectory()) {
					msgs.clear();
					File rep = XbrlDockConnUtils.findReportInFilingDir(dir, msgs);

					if ((null != rep) && rep.isFile()) {
						if (!msgs.isEmpty()) {
							XbrlDock.log(EventLevel.Warning, "Report found", id, "with warnings", msgs);
							cnt.add("Warning " + msgs);
						}

						dh.process(XDC_CMD_GEN_Init, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, filingData));

						GenAgent fh = new XbrlDockFormatAgentXhtmlReader();
//						ReportDataHandler dh = (ReportDataHandler) params[0];
						loadReport(fh, dh, filingData, rep);

					} else {
						XbrlDock.log(EventLevel.Error, "Report not found", id, dir.getPath(), filingData);
						cnt.add("Report not found " + msgs);
					}
				} else {
					XbrlDock.log(EventLevel.Error, "Filing directory not found", id, dir.getCanonicalPath(), filingData);
					cnt.add("Filing directory not found " + msgs);
				}
			}

			XbrlDock.log(EventLevel.Trace, "Visit complete", mon.getCount(), cnt);

			break;
		case XDC_CMD_GEN_TEST02:

			Collection<Map> items = (Collection) params.get(XDC_GEN_TOKEN_members);

			File targetDir = new File("work/xbrlexport");
			targetDir = new File(targetDir, XbrlDockUtils.strTime());

			GenAgent agtXhtmlReader = new XbrlDockFormatAgentXhtmlReader();
			GenAgent agtXmlWriter = null;

			for (Map filingInfo : items) {
				String id = (String) filingInfo.get("id");
				File fRep = getFiling(id);

				try (FileInputStream fr = new FileInputStream(fRep)) {
					if (null == agtXmlWriter) {
						agtXmlWriter = new XbrlDockFormatAgentXmlWriter();
						agtXmlWriter.process(XDC_CMD_GEN_Init, XbrlDockUtils.setParamMap(cp, XDC_GEN_TOKEN_target, targetDir));
					}

					agtXmlWriter.process(XDC_CMD_GEN_Begin, XbrlDockUtils.setParamMap(cp, XDC_EXT_TOKEN_id, id));
					agtXhtmlReader.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParamMap(cp, XDC_GEN_TOKEN_source, fr, XDC_GEN_TOKEN_target, agtXmlWriter));
					agtXmlWriter.process(XDC_CMD_GEN_End);
				}
			}

			// XbrlDockConnXbrlOrgTest.exportHun(dirInput, filings);

			// XbrlDockConnXbrlOrgTest.collectStats(dirStore, dirInput, filings);

			break;
		case XDC_CMD_CONN_VISITREPORT:
			String id = (String) params.get(XDC_EXT_TOKEN_id);
			GenAgent dhv = (GenAgent) params.get(XDC_GEN_TOKEN_processor);

			Map filingInfo = XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings, id);

			File dataDir = XbrlDockConnUtils.getFilingDir(dirStore, filingInfo, true, false);
			File fRep = new File(dataDir, XDC_FNAME_REPDATA);
			if (fRep.isFile()) {
				XbrlDockReportLoader.readCsv(fRep, filingInfo, dhv);
			} else {
				fRep = getFiling(id);

				GenAgent fh = new XbrlDockFormatAgentXhtmlReader();
				loadReport(fh, dhv, filingInfo, fRep);
			}
			break;
		default:
			if (null != agtConn) {
				agtConn.process(command, params);
			} else {
				XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			}
			break;
		}

		return ret;
	}

//	@Override
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

//	@Override
	public void visitReports(GenAgent visitor, GenAgent filter) throws Exception {
		XbrlDockDevMonitor pm = new XbrlDockDevMonitor("visitReports", 100);

		Map<String, Map<String, Object>> filings = XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings);

		visitor.process(XDC_CMD_GEN_Begin, null);

		for (Map.Entry<String, Map<String, Object>> fe : filings.entrySet()) {
			String k = fe.getKey();
			Map<String, Object> fd = fe.getValue();

			try {
				if (XbrlDockUtils.optCall(filter, XDC_CMD_GEN_Process, true, fd)) {
					if (pm.step()) {
//						break;
					}
					boolean cont = (boolean) visitor.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParams(XDC_EXT_TOKEN_value, fd));

					if (!cont) {
						break;
					}
				}
			} catch (Throwable t) {
				XbrlDockException.swallow(t, "Filing key", k);
			}
		}

		visitor.process(XDC_CMD_GEN_End, null);
	}

//	@Override
	public File getReportFile(String id, Object... keyPath) {
		File ret = null;

		Map filingData = XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings, id);

		if (null != filingData) {
			String localPath = (String) filingData.get((null == keyPath) ? XDC_REPORT_TOKEN_localPath : keyPath);

			if (!XbrlDockUtils.isEmpty(localPath)) {
				ret = new File(dirInput, localPath);

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
		GenAgent fh = new XbrlDockFormatAgentXhtmlReader();

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

	private void loadReport(GenAgent fh, GenAgent dh, Map<String, Object> filingData, File f)
			throws IOException, Exception, FileNotFoundException {

		if (loadReport) {
			try (FileInputStream fr = new FileInputStream(f)) {
				dh.process(XDC_CMD_GEN_Begin, XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, filingData.get(XDC_EXT_TOKEN_id)));
				fh.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParams(XDC_GEN_TOKEN_source, fr, XDC_GEN_TOKEN_target, dh));
				dh.process(XDC_CMD_GEN_End, null);
			}
		}
	}

	public File getFiling(String filingID) throws Exception {
		File ret = null;
		Map filingData = XbrlDockUtils.simpleGet(catalog, XDC_CONN_CAT_TOKEN_filings, filingID);

		String path = (String) filingData.get(XDC_REPORT_TOKEN_localFilingPath);

		if (!XbrlDockUtils.isEmpty(path)) {
			ret = new File(dirInput, path);
			if (ret.isFile()) {
				path = (String) filingData.get(XDC_REPORT_TOKEN_localMetaInfPath);
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
		path = XbrlDockUtils.sbAppend(null, File.separator, false, XDC_FNAME_CONNFILINGS, eid[0], path, eid[1], filingID).toString();

		filingData.put(XDC_REPORT_TOKEN_localPath, path);

		File fDir = new File(dirInput, path);
		XbrlDockUtilsFile.ensureDir(fDir);

		String zipFile = XbrlDockUtils.getPostfix(zipUrl, "/");
		String zipDir = XbrlDockUtils.cutPostfix(zipFile, ".");
		File fZipDir = new File(fDir, zipDir);
		File fZip = new File(fDir, zipFile);

		if (!fZipDir.isDirectory() && fZip.isFile()) {
			XbrlDock.log(EventLevel.Trace, "Unzipping package", fZip.getCanonicalPath());
			try {
				XbrlDockUtilsFile.extractWithApacheZipFile(fZipDir, fZip, null);
			} catch (Throwable t) {
				fZip.delete();
				throw t;
			}
		}

		String packStatus = XDC_CONN_PACKAGE_PROC_MSG_reportNotFound;
		str = XbrlDockUtils.simpleGet(filingData, XDC_REPORT_TOKEN_urlReport);

		boolean metaOK = rf.check(fDir);

		File fMetaInf = rf.getFile(XDC_FNAME_METAINF);

//		rf.process(null, ProcessorAction.Init);
//		XbrlDockUtilsFile.processFiles(fDir, rf, null, true, false);

		if (metaOK) {
			XbrlDockUtilsFile.storeRelativePath(dirInput, fMetaInf, filingData, XDC_REPORT_TOKEN_localMetaInfPath);
		} else {
			XbrlDock.log(EventLevel.Error, "META_INF not found", filingID, fDir.getCanonicalPath());
		}

		if (!XbrlDockUtils.isEmpty(str)) {
			String prefix = XbrlDockUtils.cutPostfix(zipUrl, "/");
			String filingFileName = str.substring(prefix.length() + 1);
			ret = new File(fZipDir, filingFileName);
			packStatus = null;
		}

		File fReports = rf.getFile(XDC_FNAME_FILINGREPORTS);

		if ((null == ret) || !ret.isFile()) {
			if (null != fReports) {
				File[] rc = fReports.listFiles(filingCandidate);

				if (0 < rc.length) {
					ret = rc[0];
					packStatus = (1 == rc.length) ? XDC_CONN_PACKAGE_PROC_MSG_reportFoundSingle : XDC_CONN_PACKAGE_PROC_MSG_reportFoundMulti;
				} else {
					repColl.process(XDC_CMD_GEN_Init, null);
					XbrlDockUtilsFile.processFiles(fReports, repColl, filingCandidate);
					Collection<File> fc = repColl.getFound();
					if (!fc.isEmpty()) {
						packStatus = XDC_CONN_PACKAGE_PROC_MSG_reportMisplaced;
						ret = fc.iterator().next();
					}
				}
			}
		}

		if ((null == ret) || !ret.isFile()) {
			if (metaOK) {
				File[] rc = fMetaInf.getParentFile().listFiles(filingCandidate);

				if (0 < rc.length) {
					ret = rc[0];
					packStatus = XDC_CONN_PACKAGE_PROC_MSG_reportMisplaced;
				}
			}
		}

		if (packStatus == XDC_CONN_PACKAGE_PROC_MSG_reportNotFound) {
			XbrlDock.log(EventLevel.Error, "Filing not found", filingID, fZipDir.getCanonicalPath(), str);
		} else {
			XbrlDockUtilsFile.storeRelativePath(dirInput, ret, filingData, XDC_REPORT_TOKEN_localFilingPath);
		}

		return ret;
	}

}
