package com.xbrldock.poc;

import java.io.File;

import com.xbrldock.XbrlDock;
import com.xbrldock.poc.meta.XbrlDockMetaContainer;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsNet;

//@SuppressWarnings({ "rawtypes" })
public class XbrlDockPocApp extends XbrlDock implements XbrlDockPocConsts {

	boolean skip  = false;
	@Override
	protected void handleLog(EventLevel level, Object... params) {
		handleLogDefault(level, params);
	}

	@Override
	protected void run() throws Exception {
		String urlCacheRoot = XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_app, XDC_CFGTOKEN_dirUrlCache);
		XbrlDockUtilsNet.setCacheRoot(urlCacheRoot);

		if (Boolean.TRUE.equals(XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_env, XDC_CFGTOKEN_AGENT_gui))) {
			callAgent(XDC_CFGTOKEN_AGENT_gui, null);
		}
		
//		if ( skip ) {
//			callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/IFRSAT-2022-03-24"));
//			callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/esef_taxonomy_2022_v1.1"));
//			return;
//		}


//		taxMgr.importTaxonomy(new File("ext/XBRLDock/temp/IFRST_2021-03-24.zip"));
		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/IFRSAT-2022-03-24"));
		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/IFRST_2021-03-24"));
		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/IFRST_2020-03-16"));
		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/IFRST_2019-03-27"));
		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/esef_taxonomy_2022_v1.1"));
		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/esef_taxonomy_2021"));
		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/esef_taxonomy_2020"));
		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/esef_taxonomy_2019"));
		
//		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/us-gaap-2024.zip"));
		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/us-gaap-2024"));
		
		if ( skip ) {
			return;
		}

		checkReports();
	}

	private void checkReports() throws Exception {
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
							XbrlDock.log(EventLevel.Info, mc.getHead());
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
