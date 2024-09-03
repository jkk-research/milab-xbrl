package com.xbrldock;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
	
	enum ReportSegment {
		Context, Unit, Fact
	}
	
	
	enum XbrlToken {
		id, scenario, context, 
		unit, unitNumerator, unitDenominator, measure,
		
		period, instant, startDate, endDate, 
		concept, entity, dimensions,
		
		value, 
		decimals, language, 
		
		continuation, 
	}	

	enum FactFldCommon {
		File, EntityId, CtxId, FactId, StartDate, EndDate, Instant, Dimensions, TagNamespace, TagId, Type, Format
	};

	enum FactFldData {
		UnitId, Unit, OrigValue, Sign, Dec, Scale, Value, Error
	};

	enum FactFldText {
		Language, Value
	};
	
	interface ReportDataHandler {
		void addNamespace(String ref, String id);
		void addTaxonomy(String tx);
		String processSegment(ReportSegment segment, Map<XbrlToken, Object> data);
	}	
	
	enum ReportFormat {
		XML, XHTML, JSON, CSV
	}
	
	interface ReportFormatHandler {
		void loadReport(Reader in, ReportDataHandler dataHandler) throws Exception;
	}
}
