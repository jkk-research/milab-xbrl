package com.xbrldock.poc.report;

import java.util.Map;

import com.xbrldock.poc.XbrlDockPocConsts;

public interface XbrlDockReportConsts extends XbrlDockPocConsts {

	String XDC_FNAME_REPDATA = "data.csv";
	String XDC_FNAME_REPTEXT = "text.csv";
	
	String XDC_EXPR_result = "xdc_exprResult";
	String XDC_EXPR_calcData = "xdc_exprCalcData";

	@SuppressWarnings("rawtypes")
	public interface ExprResultProcessor extends GenProcessor<Map> {
		boolean isByContext();
	}

}
