package com.xbrldock;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public interface XbrlDockConsts {
	
	String XBRLDOCK_SEP_FILE = "_";
	String XBRLDOCK_SEP_PATH = ".";
	String XBRLDOCK_SEP_ID = ":";

	String XBRLDOCK_CHARSET_UTF8 = StandardCharsets.UTF_8.name();// "UTF-8";
	String XBRLDOCK_FMT_TIMESTAMP = "yyyy-MM-dd'T'HH_mm_ss";
	String XBRLDOCK_FMT_DATE = "yyyy-MM-dd";
	
	String XBRLDOCK_EXT_LOG = ".log";
	String XBRLDOCK_EXT_JSON = ".json";
	String XBRLDOCK_EXT_CSV = ".csv";
	String XBRLDOCK_EXT_XML = ".xml";
	String XBRLDOCK_EXT_XHTML = ".xhtml";

	int KEY_ADD = -1;
	int KEY_SIZE = -2;
	
	public enum EventLevel {
		Exception, Error, Warning, Info, Trace, Debug
	}
	

	
	enum XbrlReportFormat {
		XML, XHTML, JSON, CSV
	}
	
	enum XbrlReportSegment {
		Context, Unit, Fact
	}
	
	enum XbrlFactDataType {
		number, string, text, date, bool, empty
	}
	
	enum XbrlToken {
		id, scenario, context, 
		unit, unitNumerator, unitDenominator, measure,
		
		period, instant, startDate, endDate, 
		concept, entity, dimensions, value, 
		
		format, decimals, scale, sign,
		
		language, continuation, 
		
		xbrldockFactType, xbrldockOrigValue, xbrldockParseError
	}	
	
	interface ReportDataHandler {
		void beginReport(String repId);
		void addNamespace(String ref, String id);
		void addTaxonomy(String tx);
		String processSegment(XbrlReportSegment segment, Map<XbrlToken, Object> data);
		void endReport();
	}	
	
	interface ReportFormatHandler {
		void loadReport(InputStream in, ReportDataHandler dataHandler) throws Exception;
	}
}
