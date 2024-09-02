package com.xbrldock;

import java.nio.charset.StandardCharsets;

public interface XbrlDockConsts {
	
	String XBRLDOCK_SEP_FILE = "_";
	String XBRLDOCK_SEP_PATH = ".";

	String XBRLDOCK_CHARSET_UTF8 = StandardCharsets.UTF_8.name();// "UTF-8";
	String XBRLDOCK_FMT_TIMESTAMP = "yyyy-MM-dd'T'HH_mm_ss";
	String XBRLDOCK_FMT_DATE = "yyyy-MM-dd";
	
	String XBRLDOCK_EXT_JSON = ".json";
	String XBRLDOCK_EXT_CSV = ".csv";
	String XBRLDOCK_EXT_XML = ".xml";

	String XBRLDOCK_CFG = "XbrlDockConfig.json";
	String XBRLDOCK_PREFIX = "xbrldock";

	int KEY_ADD = -1;
	int KEY_SIZE = -2;
	
	public enum XbrlEventLevel {
		Exception, Error, Warning, Info, Trace, Debug
	}
	
	public enum XbrlGeometry {
		location, dimension,
		x, y
	}
}
