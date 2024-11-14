package com.xbrldock.poc.format;

import com.xbrldock.poc.XbrlDockPocConsts;

public interface XbrlDockFormatConsts extends XbrlDockPocConsts {
	String XBRL_PERIOD_DATEFORMATSTR = "yyyy-MM-dd'T'HH:mm:ss";

	String XDC_FMTJSON_TOKEN_documentInfo = "documentInfo";
	String XDC_FMTJSON_TOKEN_documentType = "documentType";
	String XDC_FMTJSON_TOKEN_features = "features";
	String XDC_FMTJSON_TOKEN_namespaces = "namespaces";
	String XDC_FMTJSON_TOKEN_taxonomy = "taxonomy";
	String XDC_FMTJSON_TOKEN_facts = "facts";
	
	String XDC_FMTXML_TOKEN_xbrl = "xbrl";
	String XDC_FMTXML_TOKEN_xmlns = "xmlns";
	String XDC_FMTXML_TOKEN_dimension = "dimension";
	
	String XDC_FMTXML_TOKEN_explicitMember = "explicitMember";
	String XDC_FMTXML_TOKEN_contextRef = "contextRef";
	String XDC_FMTXML_TOKEN_unitRef = "unitRef";

}
