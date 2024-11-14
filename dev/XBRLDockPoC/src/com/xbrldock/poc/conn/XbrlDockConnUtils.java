package com.xbrldock.poc.conn;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlDockConnUtils implements XbrlDockConnConsts {

	public static class DirMapper implements XbrlDockUtilsFile.FileProcessor {
		Map<String, File> specFiles = new TreeMap<String, File>();

		@Override
		public Object process(String cmd, Object... params) throws Exception {
			switch (cmd) {
			case XDC_CMD_GEN_Init:
				specFiles.clear();
				break;
			case XDC_CMD_GEN_Begin:
				File f = (File) params[0];
				switch (f.getName()) {
				case XDC_FNAME_METAINF:
					File fCat = new File(f, XDC_FNAME_FILINGCATALOG);
					File fTp = new File(f, XDC_FNAME_FILINGTAXPACK);

					if (fCat.isFile() && fTp.isFile()) {
						specFiles.put(XDC_FNAME_METAINF, f);
						specFiles.put(XDC_FNAME_FILINGCATALOG, fCat);
						specFiles.put(XDC_FNAME_FILINGTAXPACK, fTp);
					}
					break;
				case XDC_FNAME_FILINGREPORTS:
					specFiles.put(XDC_FNAME_FILINGREPORTS, f);
					break;
				}
				break;
			default:
				break;
			}

			return true;
		}

		public boolean check(File fDir) throws Exception {
			process(XDC_CMD_GEN_Init);
			XbrlDockUtilsFile.processFiles(fDir, this, null, true, false);
			return isOK();
		}

		public boolean isOK() {
			return specFiles.containsKey(XDC_FNAME_METAINF);
		}

		public File getFile(String type) {
			return specFiles.get(type);
		}
	};

	public static File getFilingDir(File root, Map filingData, boolean addHash, boolean createIfMissing) throws Exception {
		String entityId = (String) filingData.get(XDC_REPORT_TOKEN_entityId);
		String filingId = (String) filingData.get(XDC_EXT_TOKEN_id);

		String[] eid = entityId.split(XDC_SEP_ID); // idType:idValue !!
		String path = addHash ? XbrlDockUtils.getHash2(eid[1], File.separator) : "";
		path = XbrlDockUtils.sbAppend(null, File.separator, false, eid[0], path, eid[1], filingId).toString();

		filingData.put(XDC_REPORT_TOKEN_localPath, path);

		File fDir = new File(root, path);
		if (createIfMissing) {
			XbrlDockUtilsFile.ensureDir(fDir);
		}
		return fDir;
	};

	public static File findReportInFilingDir(File fDir, Collection<String> msgs) throws Exception {
		File ret = null;

		XbrlDockConnUtils.DirMapper rf = new XbrlDockConnUtils.DirMapper();

		boolean metaOK = rf.check(fDir);

		File fMetaInf = rf.getFile(XDC_FNAME_METAINF);

		if (!metaOK) {
			msgs.add(XDC_CONN_PACKAGE_PROC_MSG_metaInfNotFound);
		}

		File fReports = rf.getFile(XDC_FNAME_FILINGREPORTS);

		if (null != fReports) {
			File[] rc = fReports.listFiles(XBRL_FILTER);

			if (0 < rc.length) {
				ret = rc[0];
				if (1 < rc.length) {
					msgs.add(XDC_CONN_PACKAGE_PROC_MSG_reportFoundMulti);
				}
			} else {
				XbrlDockUtilsFile.FileCollector repColl = new XbrlDockUtilsFile.FileCollector();

				repColl.process(XDC_CMD_GEN_Init);
				XbrlDockUtilsFile.processFiles(fReports, repColl, XBRL_FILTER);
				Collection<File> fc = repColl.getFound();
				if (!fc.isEmpty()) {
					ret = fc.iterator().next();

					msgs.add(XDC_CONN_PACKAGE_PROC_MSG_reportMisplaced);

					if (1 < fc.size()) {
						msgs.add(XDC_CONN_PACKAGE_PROC_MSG_reportFoundMulti);
					}
				}
			}
		}

		if ((null == ret) || !ret.isFile()) {
			if (metaOK) {
				File[] rc = fMetaInf.getParentFile().listFiles(XBRL_FILTER);

				if (0 < rc.length) {
					ret = rc[0];
					msgs.add(XDC_CONN_PACKAGE_PROC_MSG_reportMisplaced);

					if (1 < rc.length) {
						msgs.add(XDC_CONN_PACKAGE_PROC_MSG_reportFoundMulti);
					}
				}
			}
		}

		return ret;
	}

}
