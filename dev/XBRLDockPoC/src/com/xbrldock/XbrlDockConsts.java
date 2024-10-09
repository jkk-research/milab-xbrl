package com.xbrldock;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@SuppressWarnings("rawtypes")
public interface XbrlDockConsts {
	
	String XDC_SEP_FILE = "_";
	String XDC_SEP_PATH = ".";
	String XDC_SEP_ID = ":";
	String XDC_SEP_ITEMID = "#";

	String XDC_CHARSET_UTF8 = StandardCharsets.UTF_8.name();// "UTF-8";
	String XDC_FMT_TIMESTAMP = "yyyy-MM-dd'T'HH_mm_ss";
	String XDC_FMT_DATE = "yyyy-MM-dd";
	
	String XDC_FEXT_ZIP = ".zip";
	String XDC_FEXT_LOG = ".log";
	String XDC_FEXT_JSON = ".json";
	String XDC_FEXT_CSV = ".csv";
	String XDC_FEXT_XML = ".xml";
	String XDC_FEXT_XHTML = ".xhtml";
	String XDC_FEXT_HTML = ".html";
	String XDC_FEXT_XBRL = ".xbrl";
	String XDC_FEXT_SCHEMA = ".xsd";
	
	String XDC_URL_PSEP = "://";
	String XDC_URL_UP = "../";
	String XDC_URL_HERE = "./";
	String XDC_PATH_UP = ".." + File.separator;
	
	String XDC_FNAME_CONFIG = "XbrlDockConfig.json";
	String XDC_CFGPREFIX_xbrldock = "xbrldock";
	
	String XDC_CFGTOKEN_env = "env";
	
	String XDC_CFGTOKEN_javaClass = "javaClass";
	String XDC_CFGTOKEN_app = "app";
	String XDC_CFGTOKEN_gui = "gui";
	String XDC_CFGTOKEN_agents = "agents";
	String XDC_CFGTOKEN_dirUrlCache = "dirUrlCache";
	String XDC_CFGTOKEN_ = "";


	int KEY_ADD = -1;
	int KEY_SIZE = -2;
	
	public enum EventLevel {
		Exception, Error, Warning, Info, Trace, Debug, Context
	}
	
	public enum ProcessorAction {
		Init, Begin, Process, End, Release
	}
	
//	public interface GenApp {
//		GenAgent getAgent(String agentId) throws Exception;
//		<RetType> RetType callAgent(String agentId, String command, Object... params) throws Exception;
//	}
//	
	public interface GenAgent {
		void initModule(Map config) throws Exception;
		<RetType> RetType process(String command, Object... params) throws Exception;
	}
	
	interface ItemCreator<Type> {
		Type create(Object key, Object... hints);
	}
	
	public interface GenProcessor<ItemType> {
		boolean process(ItemType item, ProcessorAction action) throws Exception;
	}

	String XDC_FORMAT_XML = "XML";
	String XDC_FORMAT_XHTML = "XHTML";
	String XDC_FORMAT_JSON = "JSON";
	String XDC_FORMAT_CSV = "CSV";
	
	
	String XDC_REP_SEG_Context = "Context";
	String XDC_REP_SEG_Unit = "Unit";
	String XDC_REP_SEG_Fact = "Fact";
	
			
	String XDC_FACT_VALTYPE_number = "number";
	String XDC_FACT_VALTYPE_string = "string";
	String XDC_FACT_VALTYPE_text = "text";
	String XDC_FACT_VALTYPE_date = "date";
	String XDC_FACT_VALTYPE_bool = "bool";
	String XDC_FACT_VALTYPE_empty = "empty";
	
	
	String XDC_ENTITY_ID_TYPE_ = "";
	String XDC_ENTITY_ID_TYPE_LEI = "lei";

	String XDC_GEN_TOKEN_id = "id";
	String XDC_GEN_TOKEN_name = "name";
	String XDC_GEN_TOKEN_value = "value";
	String XDC_GEN_TOKEN_members = "members";

	String XDC_ENTITY_TOKEN_idType = "idType";
	String XDC_ENTITY_TOKEN_urlSource = "urlSource";

	String XDC_REPORT_TOKEN_source = "source";
	String XDC_REPORT_TOKEN_periodEnd = "periodEnd";
	String XDC_REPORT_TOKEN_published = "published";
	String XDC_REPORT_TOKEN_entityId = "entityId";
	String XDC_REPORT_TOKEN_entityName = "entityName";
	String XDC_REPORT_TOKEN_langCode = "langCode";
	String XDC_REPORT_TOKEN_schemas = "schemas";
	String XDC_REPORT_TOKEN_namespaces = "namespaces";
	
	String XDC_REPORT_TOKEN_urlPackage = "urlPackage";
	String XDC_REPORT_TOKEN_sourceUrl = "sourceUrl";
	String XDC_REPORT_TOKEN_sourceAtts = "sourceAtts";
	
	String XDC_REPORT_TOKEN_packageStatus = "packageStatus";
	String XDC_REPORT_TOKEN_startDate = "startDate";
	String XDC_REPORT_TOKEN_endDate = "endDate";
	String XDC_REPORT_TOKEN_localPath = "localPath";
	String XDC_REPORT_TOKEN_localFilingPath = "localFilingPath";
	String XDC_REPORT_TOKEN_localMetaInfPath = "localMetaInfPath";

	
	String XDC_FACT_TOKEN_scenario = "scenario";
	String XDC_FACT_TOKEN_context = "context";
	
	String XDC_FACT_TOKEN_unit = "unit";
	String XDC_FACT_TOKEN_unitNumerator = "unitNumerator";
	String XDC_FACT_TOKEN_unitDenominator = "unitDenominator";
	String XDC_FACT_TOKEN_measure = "measure";
	
	String XDC_FACT_TOKEN_period = "period";
	String XDC_FACT_TOKEN_instant = "instant";
	String XDC_FACT_TOKEN_startDate = "startDate";
	String XDC_FACT_TOKEN_endDate = "endDate";
	
	String XDC_FACT_TOKEN_concept = "concept";
	String XDC_FACT_TOKEN_entity = "entity";
	String XDC_FACT_TOKEN_dimensions = "dimensions";
	
	String XDC_FACT_TOKEN_format = "format";
	String XDC_FACT_TOKEN_decimals = "decimals";
	String XDC_FACT_TOKEN_scale = "scale";
	String XDC_FACT_TOKEN_sign = "sign";
	
	String XDC_FACT_TOKEN_language = "language";
	String XDC_FACT_TOKEN_continuation = "continuation";
	String XDC_FACT_TOKEN_xbrldockFactType = "xbrldockFactType";
	String XDC_FACT_TOKEN_xbrldockOrigValue = "xbrldockOrigValue";
	String XDC_FACT_TOKEN_xbrldockParseError = "xbrldockParseError";
	
}
