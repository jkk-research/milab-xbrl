package com.xbrldock.poc;

import java.io.File;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.meta.XbrlDockMetaContainer;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsNet;

//@SuppressWarnings({ "rawtypes" })
public class XbrlDockPocApp extends XbrlDock implements XbrlDockPocConsts {

	boolean skip = false;
	EventLevel logAbove = EventLevel.Trace;

	Object[] logContext = null;

	@Override
	protected void handleLog(EventLevel level, Object... params) {
		if (EventLevel.Context == level) {
			logContext = params;
		} else {
			if (0 > level.compareTo(logAbove)) {
				if (null != logContext) {
					System.out.println();
					handleLogDefault(EventLevel.Context, logContext);
					logContext = null;
				}
				handleLogDefault(level, params);
			}
		}
	}

	@Override
	protected void run() throws Exception {
		String urlCacheRoot = XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_app, XDC_CFGTOKEN_dirUrlCache);
		XbrlDockUtilsNet.setCacheRoot(urlCacheRoot);

//		XbrlDockException.DUMP_STACK_TRACE = null;
		
		if (Boolean.TRUE.equals(XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_env, XDC_CFGTOKEN_AGENT_gui))) {
			callAgent(XDC_CFGTOKEN_AGENT_gui, XDC_CMD_WORKBENCH_SELECT, XDC_CFGTOKEN_AGENT_metaManager);
			return;
		}

//		@formatter:off  
		String[] years = { 
				"2019", 
				"2020", 
				"2021", 
				"2022", 
				};
//		@formatter:on

		for (String year : years) {
			switch (year) {
			case "2019":
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/IFRST_2019-03-27"));
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/esef_taxonomy_2019"));
				break;
			case "2020":
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/IFRST_2020-03-16"));
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/esef_taxonomy_2020"));
				break;
			case "2021":
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/IFRST_2021-03-24"));
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/esef_taxonomy_2021"));
				break;
			case "2022":
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/IFRSAT-2022-03-24"));
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/esef_taxonomy_2022_v1.1"));
				break;
			}
		}
//

//		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/us-gaap-2024"));

		checkReports();

//		checkReport(
//				"ext/XBRLDock/sources/xbrl.org/filings/lei/32/bd/74780000L0GQ5QG49R37/74780000L0GQ5QG49R37-2022-12-31-ESEF-HR-1/74780000L0GQ5QG49R37-2022-12-31/ATPL-2022-12-31-hr/META-INF");
	}

	void checkReport(String repRoot) throws Exception {
		File f = new File(repRoot);

		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_GETMC, f);
	}

	void checkReports() throws Exception {
		String srcRoot = XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_app, XDC_CFGTOKEN_agents, XDC_CFGTOKEN_AGENT_esefConn, XDC_CFGTOKEN_dirInput);
		File root = new File(srcRoot);

		XbrlDockUtilsFile.FileProcessor mif = new XbrlDockUtilsFile.FileProcessor() {
			@Override
			public boolean process(File f, ProcessorAction action) {
				if ((action == ProcessorAction.Begin) && XbrlDockUtils.isEqual(XDC_FNAME_METAINF, f.getName())) {
					if (new File(f, XDC_FNAME_CATALOG).isFile()) {
//						XbrlDock.log(EventLevel.Trace, "MIF found", f.getPath());

						try {
//							XbrlDockPocUtils.readMeta(f);
							XbrlDockMetaContainer mc = callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_GETMC, f);
//							XbrlDock.log(EventLevel.Info, mc.getHead());
						} catch (Throwable e) {
							XbrlDock.log(EventLevel.Trace, "load exception", e);
						}
					} else {
						XbrlDock.log(EventLevel.Warning, "No catalog.xml in META-INF", f.getPath());
					}
				}
				return true;
			}
		};

		XbrlDockUtilsFile.processFiles(root, mif, null, true, false);

	}

}
