package com.xbrldock.poc;

import java.io.File;

import com.xbrldock.XbrlDock;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsNet;

//@SuppressWarnings({ "rawtypes" })
public class XbrlDockPocApp extends XbrlDock implements XbrlDockPocConsts {

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
		
//		taxMgr.importTaxonomy(new File("ext/XBRLDock/temp/IFRST_2021-03-24.zip"));
		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/2024-09-26T16_13_08"));
//		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/esef_taxonomy_2021"));
//		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/us-gaap-2024.zip"));
//		callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_METAMGR_IMPORT, new File("ext/XBRLDock/taxonomy/import/2024-10-02T04_36_16"));
		
	}

}
