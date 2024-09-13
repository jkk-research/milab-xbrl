package com.xbrldock.poc;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.utils.XbrlDockUtilsConsts;

public interface XbrlDockPocConsts extends XbrlDockConsts, XbrlDockUtilsConsts {
	String XBRLDOCK_CFG = "XbrlDockConfig.json";
	String XBRLDOCK_PREFIX = "xbrldock";

	enum ConfigKey {
		location, dimension, x, y, gui, frame
	}

}
