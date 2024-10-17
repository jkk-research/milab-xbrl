package com.xbrldock.poc.conn;

import java.io.File;
import java.io.FileFilter;

import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.poc.format.XbrlDockFormatConsts;
import com.xbrldock.poc.format.XbrlDockFormatUtils;

public interface XbrlDockConnConsts extends XbrlDockPocConsts, XbrlDockFormatConsts {

	String XDC_FNAME_CONNCATALOG = "catalog.json";
	
	String XDC_FNAME_CONNFILINGS = "filings";
	
	String XDC_CONN_CAT_TOKEN_languages = "languages";
	String XDC_CONN_CAT_TOKEN_entities = "entities";
	String XDC_CONN_CAT_TOKEN_filings = "filings";

	String XDC_CONN_PACKAGE_PROC_MSG_metaInfNotFound = "metaInfNotFound";
	String XDC_CONN_PACKAGE_PROC_MSG_reportFoundSingle = "reportFoundSingle";
	String XDC_CONN_PACKAGE_PROC_MSG_reportFoundMulti = "reportFoundMulti";
	String XDC_CONN_PACKAGE_PROC_MSG_reportMisplaced = "reportMisplaced";
	String XDC_CONN_PACKAGE_PROC_MSG_reportNotFound = "reportNotFound";
	
	FileFilter XBRL_FILTER = new FileFilter() {
		@Override
		public boolean accept(File f) {
			return XbrlDockFormatUtils.canBeXbrl(f);
		}
	};

}
