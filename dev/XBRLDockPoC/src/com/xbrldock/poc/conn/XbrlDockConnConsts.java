package com.xbrldock.poc.conn;

import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.poc.format.XbrlDockFormatConsts;

public interface XbrlDockConnConsts extends XbrlDockPocConsts, XbrlDockFormatConsts {

	String PATH_CATALOG = "catalog.json";
	
	String PATH_FILING_CACHE = "filings";
	String PATH_DATA = "data";
	
	String XDC_CONN_CAT_TOKEN_languages = "languages";
	String XDC_CONN_CAT_TOKEN_entities = "entities";
	String XDC_CONN_CAT_TOKEN_filings = "filings";

	String XDC_CONN_PACKAGE_PROC_MSG_reportFoundSingle = "reportFoundSingle";
	String XDC_CONN_PACKAGE_PROC_MSG_reportFoundMulti = "reportFoundMulti";
	String XDC_CONN_PACKAGE_PROC_MSG_reportMisplaced = "reportMisplaced";
	String XDC_CONN_PACKAGE_PROC_MSG_reportNotFound = "reportNotFound";
	
}
