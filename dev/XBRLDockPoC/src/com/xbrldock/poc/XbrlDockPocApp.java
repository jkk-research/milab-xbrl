package com.xbrldock.poc;

import java.io.File;

import com.xbrldock.XbrlDock;
import com.xbrldock.utils.XbrlDockUtils;

//@SuppressWarnings({ "rawtypes" })
public class XbrlDockPocApp extends XbrlDock implements XbrlDockPocConsts {

	@Override
	protected void handleLog(EventLevel level, Object... params) {
		handleLogDefault(level, params);
	}
	
	@Override
	protected void run() throws Exception {
		getModule(XDC_CFGTOKEN_MOD_urlCache);
	 	XDModTaxonomyManager taxMgr = getModule(XDC_CFGTOKEN_MOD_taxmgr);
		getModule(XDC_CFGTOKEN_MOD_esefConn);

		if (Boolean.TRUE.equals(XbrlDockUtils.simpleGet(APP_CONFIG, XDC_CFGTOKEN_env, XDC_CFGTOKEN_MOD_gui))) {
			getModule(XDC_CFGTOKEN_MOD_gui);
		}
		
		taxMgr.importTaxonomy(new File("ext/XBRLDock/temp/IFRST_2021-03-24.zip"));
	}

}
