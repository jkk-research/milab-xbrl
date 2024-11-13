package com.xbrldock.poc;

import java.io.File;

import com.xbrldock.XbrlDock;
import com.xbrldock.poc.meta.XbrlDockMetaContainer;
import com.xbrldock.poc.meta.XbrlDockMetaManager;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsNet;

//@SuppressWarnings({ "rawtypes" })
public class XbrlDockPocApp extends XbrlDock implements XbrlDockPocConsts {

	boolean skip = true;

	@Override
	public Object process(String cmd, Object... params) throws Exception {
		switch ( cmd ) {
		case XDC_CMD_GEN_Begin:
			run();
			break;
		}
		return null;
	}
	
//	@Override
	protected void run() throws Exception {
		
//		File srcRoot = new File("work/input");
//		File targetRoot = new File("/Volumes/Backup01/lkedves/XBRLDock");
//		
//		XbrlDockUtilsFile.backup(srcRoot, targetRoot);
		
		String urlCacheRoot = XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_app, XDC_CFGTOKEN_dirUrlCache);
		XbrlDockUtilsNet.setCacheRoot(urlCacheRoot);
		
		boolean gui = Boolean.TRUE.equals( XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_app, XDC_CFGTOKEN_gui));

		if (gui || Boolean.TRUE.equals(XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_env, XDC_CFGTOKEN_AGENT_gui))) {
			XbrlDockMetaManager.LOAD_CACHE = true;
			callAgent(XDC_CFGTOKEN_AGENT_gui, XDC_CMD_GEN_SELECT, XDC_CFGTOKEN_AGENT_metaManager);
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
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("work/input/taxonomies/IFRST_2019-03-27"));
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("work/input/taxonomies/esef_taxonomy_2019"));
				break;
			case "2020":
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("work/input/taxonomies/IFRST_2020-03-16"));
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("work/input/taxonomies/esef_taxonomy_2020"));
				break;
			case "2021":
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("work/input/taxonomies/IFRST_2021-03-24"));
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("work/input/taxonomies/esef_taxonomy_2021"));
				break;
			case "2022":
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("work/input/taxonomies/IFRSAT-2022-03-24"));
				callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("work/input/taxonomies/esef_taxonomy_2022_v1.1"));
				break;
			}
		}
//

//		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("work/input/taxonomies/us-gaap-2024"));

//		if ( skip ) {
//			return;
//		}
		
//		XbrlDockException.DUMP_STACK_TRACE = null;
//		logAbove = EventLevel.Trace;
//		checkReports();

//		checkReport(
//				"ext/XBRLDock/sources/xbrl.org/filings/lei/25/7a/549300HB18MY7I4M4L84/549300HB18MY7I4M4L84-2023-12-31-ESEF-DK-0/549300HB18MY7I4M4L84-2023-12-31-en/549300HB18MY7I4M4L84-2023-12-31-en/META-INF");
	}

	void checkReport(String repRoot) throws Exception {
		File f = new File(repRoot);

		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_LOADMC, f);
	}

	void checkReports() throws Exception {
		String srcRoot = XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_app, XDC_CFGTOKEN_agents, XDC_CFGTOKEN_AGENT_esefConn, XDC_CFGTOKEN_dirInput);
		File root = new File(srcRoot);

		XbrlDockUtilsFile.FileProcessor mif = new XbrlDockUtilsFile.FileProcessor() {
			@Override
			public Object process(String cmd, Object... params) throws Exception {
				File f = (File) params[0];
				if (XDC_CMD_GEN_Begin.equals(cmd) && XDC_FNAME_METAINF.equals(f.getName())) {
					if (new File(f, XDC_FNAME_FILINGCATALOG).isFile()) {
//						XbrlDock.log(EventLevel.Trace, "MIF found", f.getPath());

						try {
//							XbrlDockPocUtils.readMeta(f);
							XbrlDockMetaContainer mc = callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_LOADMC, f);
							XbrlDock.log(EventLevel.Info, mc.getId());
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
