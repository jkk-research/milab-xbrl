package com.xbrldock.poc;

import java.util.Map;

import com.xbrldock.XbrlDock;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsJson;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlDockPoc extends XbrlDock implements XbrlDockPocConsts {

	@Override
	protected void handleLog(XbrlEventLevel level, Object... params) {
		handleLogDefault(level, params);
	}

	public void initEnv(String[] args) throws Exception {
		Object cfgData = XbrlDockUtilsJson.readJson(XBRLDOCK_CFG);
		
		Map cfg = XbrlDockUtils.toFlatMap(XBRLDOCK_PREFIX, XBRLDOCK_SEP_PATH, cfgData);
		
		initEnv(args, cfg);
	}
	
}
