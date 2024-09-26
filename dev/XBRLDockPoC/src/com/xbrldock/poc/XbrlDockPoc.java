package com.xbrldock.poc;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevReportDump;
import com.xbrldock.poc.conn.xbrlorg.XbrlDockConnXbrlOrg;
import com.xbrldock.poc.format.XbrlDockFormatJson;
import com.xbrldock.poc.format.XbrlDockFormatXhtml;
import com.xbrldock.poc.gui.XbrlDockGuiApp;
import com.xbrldock.poc.taxonomy.XbrlDockTaxonomy;
import com.xbrldock.poc.taxonomy.XbrlDockTaxonomyManager;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;

//@SuppressWarnings({ "unchecked", "rawtypes" })
@Deprecated
public class XbrlDockPoc extends XbrlDock implements XbrlDockPocConsts {
	public final File testRoot = new File("/Volumes/Giskard_ext/work/XBRL/store/xbrl.org/reports/lei");
	public final File localRoot = new File("temp/reports");
	public final String cacheRoot = "../../urlCache";

	XbrlDockTaxonomyManager taxMgr;
	XbrlDockConnXbrlOrg esefConn;
	
	public static void main(String[] args) {
		try {
			XbrlDockPoc xbrlDock = new XbrlDockPoc();

			xbrlDock.initEnv(args);

			xbrlDock.test();
		} catch (Throwable t) {
			XbrlDock.log(EventLevel.Exception, t);
		}
	}
	
	public XbrlDockTaxonomyManager getTaxMgr() {
		return taxMgr;
	}

	

	@Override
	protected void handleLog(EventLevel level, Object... params) {
		handleLogDefault(level, params);
	}

	public void initEnv(String[] args) throws Exception {
//		Object cfgData = XbrlDockUtilsJson.readJson(XDC_FNAME_CONFIG);

//		Map cfg = XbrlDockUtils.toFlatMap(XDC_PREFIX, XDC_SEP_PATH, cfgData);
//
//		initEnv(XDC_PREFIX, args, cfg);

//		esefConn = new XbrlDockConnXbrlOrg("sources/xbrl.org", "ext/XBRLDock/sources/xbrl.org");
//		
//		taxMgr = new XbrlDockTaxonomyManager("temp/taxonomy", cacheRoot);

	}

	public boolean test() throws Exception {
		XbrlDockDevReportDump dh = new XbrlDockDevReportDump();

		FileFilter xhtmlFilter = new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.getName().endsWith("html") && XbrlDockUtils.isEqual(f.getParentFile().getName(), "reports");
			}
		};
		ReportFormatHandler xhtmlParser = new XbrlDockFormatXhtml();

		FileFilter jsonFilter = new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(XDC_FEXT_JSON);
			}
		};
		ReportFormatHandler jsonParser = new XbrlDockFormatJson();

		String mode = "";

		mode = "app";
//		mode = "taxonomy";
//		 mode = "esef";
//		 mode = "xhtmlRec";
//		mode = "jsonRec";
//		 mode = "jsonSingle";
//		 mode = "xhtmlSingle";

		switch (mode) {
		case "xhtmlSingle":
			loadReport("549300EW945NUS7PK214-2022-12-31-en/reports/549300EW945NUS7PK214-2022-12-31-en.xhtml", xhtmlParser, dh);
			break;
		case "jsonSingle":
			loadReport("549300EW945NUS7PK214-2022-12-31-en.json", jsonParser, dh);
			break;
		case "xhtmlRec":
			dh.logAll = false;
			loadReportRec(xhtmlFilter, xhtmlParser, dh);
			break;
		case "jsonRec":
			dh.logAll = false;
			loadReportRec(jsonFilter, jsonParser, dh);
			break;
		case "esef":
//			taxMgr = new XbrlDockTaxonomyManager("temp/taxonomy", "ext/XBRLDock/urlcache");
			esefConn.test();
			break;
		case "taxonomy":
			
			XbrlDockTaxonomy txIfrs2024 = taxMgr.loadTaxonomy("xbrl.ifrs.org/taxonomy/2024-03-27");
			
			txIfrs2024.getRes("en");

			break;
		case "app":
			XbrlDockGuiApp frame = new XbrlDockGuiApp(this);

			frame.showTaxonomy("/xbrl.ifrs.org/taxonomy/2024-03-27/");

			break;
		default:
			return true;
		}

		return false;
	}

	void loadReportRec(FileFilter filter, ReportFormatHandler fh, ReportDataHandler dh) throws Exception, IOException, FileNotFoundException {

		XbrlDockUtilsFile.FileProcessor processor = new XbrlDockUtilsFile.FileProcessor() {
			@Override
			public boolean process(File f, ProcessorAction action) {
				try (InputStream fr = new FileInputStream(f)) {
					loadReport(f, fh, dh);
					return true;
				} catch (Exception e) {
					XbrlDockException.swallow(e);
					return false;
				}
			}
		};

		long ts = System.currentTimeMillis();
		int count = XbrlDockUtilsFile.processFiles(testRoot, processor, filter);

		XbrlDock.log(EventLevel.Info, "Loaded", count, "files in", System.currentTimeMillis() - ts, "msec.");
	}

	void loadReport(String localFile, ReportFormatHandler fh, ReportDataHandler dh) throws Exception, IOException, FileNotFoundException {
		File f = new File(localRoot, localFile);

		if (!f.isFile()) {
			XbrlDock.log(EventLevel.Error, "Missing local file", f.getCanonicalPath());
		} else {
			loadReport(f, fh, dh);
		}
	}

	void loadReport(File f, ReportFormatHandler fh, ReportDataHandler dh) throws Exception, IOException, FileNotFoundException {
		try (FileInputStream fr = new FileInputStream(f)) {
			dh.beginReport(f.getCanonicalPath());
			fh.loadReport(fr, dh);
			dh.endReport();
		}
	}

	@Override
	protected void run() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
