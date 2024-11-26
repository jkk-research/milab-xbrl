package com.xbrldock.poc.report;

import com.xbrldock.poc.XbrlDockPocConsts;

public interface XbrlDockReportConsts extends XbrlDockPocConsts {
	
	String XDC_FNAME_CONNCATALOG = "catalog.json";
	String XDC_FNAME_CONNFILINGS = "filings";

	String XDC_STORE_CONNECTOR = "connector";

	String XDC_CONN_CAT_TOKEN_languages = "languages";
	String XDC_CONN_CAT_TOKEN_entities = "entities";
	String XDC_CONN_CAT_TOKEN_filings = "filings";
	
	String XDC_CONN_PACKAGE_PROC_MSG_metaInfNotFound = "metaInfNotFound";
	String XDC_CONN_PACKAGE_PROC_MSG_reportFoundSingle = "reportFoundSingle";
	String XDC_CONN_PACKAGE_PROC_MSG_reportFoundMulti = "reportFoundMulti";
	String XDC_CONN_PACKAGE_PROC_MSG_reportMisplaced = "reportMisplaced";
	String XDC_CONN_PACKAGE_PROC_MSG_reportNotFound = "reportNotFound";


	String XDC_FNAME_REPDATA = "data.csv";
	String XDC_FNAME_REPTEXT = "text.csv";
	
	String XDC_EXPR_result = "xdc_exprResult";
	String XDC_EXPR_calcData = "xdc_exprCalcData";

	public interface ExprResultProcessor extends GenAgent {
		boolean isByContext();
	}

}
