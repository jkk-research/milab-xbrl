package com.xbrldock.poc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.format.XbrlDockFormatJson;
import com.xbrldock.poc.format.XbrlDockFormatXhtml;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsDumpReportHandler;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsJson;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlDockPoc extends XbrlDock implements XbrlDockPocConsts {
	File dir = new File("/Volumes/Giskard_ext/work/XBRL/store/xbrl.org/reports/lei");

	@Override
	protected void handleLog(XbrlEventLevel level, Object... params) {
		handleLogDefault(level, params);
	}

	public void initEnv(String[] args) throws Exception {
		Object cfgData = XbrlDockUtilsJson.readJson(XBRLDOCK_CFG);

		Map cfg = XbrlDockUtils.toFlatMap(XBRLDOCK_PREFIX, XBRLDOCK_SEP_PATH, cfgData);

		initEnv(args, cfg);
		ReportDataHandler dh = new XbrlDockUtilsDumpReportHandler();

//		loadRecJson(dh, dir);
//		loadSingleJson(dh);
		loadSingleXhtml(dh);

	}

	void loadSingleJson(ReportDataHandler dh) throws Exception, IOException, FileNotFoundException {
		try (FileInputStream fr = new FileInputStream("temp/reports/549300EW945NUS7PK214-2022-12-31-en.json")) {
			ReportFormatHandler fh = new XbrlDockFormatJson();
			fh.loadReport(fr, dh);
		}
	}

	void loadRecJson(ReportDataHandler dh, File dir) {
		XbrlDockUtilsFile.FileProcessor jsonFilter = new XbrlDockUtilsFile.FileProcessor() {
			@Override
			public boolean process(File f) {
				return f.getName().endsWith(XBRLDOCK_EXT_JSON);
			}
		};

		XbrlDockUtilsFile.FileProcessor jsonReader = new XbrlDockUtilsFile.FileProcessor() {
			@Override
			public boolean process(File f) {
				try (InputStream fr = new FileInputStream(f)) {
					XbrlDock.log(XbrlEventLevel.Info, "Reading file", f.getCanonicalPath());

					ReportFormatHandler fh = new XbrlDockFormatJson();
					fh.loadReport(fr, dh);
					return true;
				} catch (Exception e) {
					XbrlDockException.swallow(e);
					return false;
				}
			}
		};

		XbrlDockUtilsFile.processFiles(dir, jsonReader, jsonFilter);
	}

	void loadSingleXhtml(ReportDataHandler dh) throws Exception, IOException, FileNotFoundException {
		try (FileInputStream fr = new FileInputStream(
				"temp/reports/549300EW945NUS7PK214-2022-12-31-en/reports/549300EW945NUS7PK214-2022-12-31-en.xhtml")) {
			ReportFormatHandler fh = new XbrlDockFormatXhtml();
			fh.loadReport(fr, dh);
		}
	}

}
