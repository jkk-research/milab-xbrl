package com.xbrldock;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public interface XbrlDockConsts {

	public enum EventLevel {
		Exception, Error, Warning, Info, Trace, Debug, Context
	}

	String XDC_CMD_GEN_Init = "xdc_Init";
	String XDC_CMD_GEN_Begin = "xdc_Begin";
	String XDC_CMD_GEN_Process = "xdc_Process";
	String XDC_CMD_GEN_End = "xdc_End";
	String XDC_CMD_GEN_Release = "xdc_Release";
	
	String XDC_CMD_GEN_SAVE = "save";
	String XDC_CMD_GEN_LOAD = "load";
	String XDC_CMD_GEN_REFRESH = "refresh";
	String XDC_CMD_GEN_STOP = "stop";
	String XDC_CMD_GEN_DELETE = "delete";
	String XDC_CMD_GEN_TEST01 = "test01";
	String XDC_CMD_GEN_TEST02 = "test02";


	public interface GenAgent {
		Object process(String cmd, Map<String, Object> params) throws Exception;

		@SuppressWarnings("unchecked")
		default Object process(String cmd) throws Exception {
			return process(cmd, Collections.EMPTY_MAP);
		}
	}

	interface ItemCreator<Type> {
		Type create(Object key, Object... hints);
	}


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

	String XDC_CFGTOKEN_app = "app";
	String XDC_CFGTOKEN_userFlags = "userFlags";
	String XDC_CFGTOKEN_gui = "gui";
	String XDC_CFGTOKEN_javaClass = "javaClass";
	String XDC_CFGTOKEN_agents = "agents";
	String XDC_CFGTOKEN_dirUrlCache = "dirUrlCache";
	String XDC_CFGTOKEN_ = "";

	String XDC_FLAG_ADMIN = "admin";

	int KEY_ADD = -1;
	int KEY_SIZE = -2;

	String XDC_FORMAT_XML = "XML";
	String XDC_FORMAT_XHTML = "XHTML";
	String XDC_FORMAT_JSON = "JSON";
	String XDC_FORMAT_CSV = "CSV";
	
	String XDC_CMD_REP_ADD_NAMESPACE = "xdc_Namespace";
	String XDC_CMD_REP_ADD_SCHEMA = "xdc_Schema";

	String XDC_REP_SEG_Context = "Context";
	String XDC_REP_SEG_Unit = "Unit";
	String XDC_REP_SEG_Fact = "Fact";
	String[] XDC_SEGMENTS = {XDC_REP_SEG_Context, XDC_REP_SEG_Unit, XDC_REP_SEG_Fact};

	String XDC_FACT_VALTYPE_number = "number";
	String XDC_FACT_VALTYPE_string = "string";
	String XDC_FACT_VALTYPE_textClip = "textClip";
	String XDC_FACT_VALTYPE_text = "text";
	String XDC_FACT_VALTYPE_date = "date";
	String XDC_FACT_VALTYPE_bool = "bool";
	String XDC_FACT_VALTYPE_empty = "empty";

	String XDC_ENTITY_ID_TYPE_ = "";
	String XDC_ENTITY_ID_TYPE_LEI = "lei";

	String XDC_GEN_TOKEN_members = "members";
	String XDC_GEN_TOKEN_requires = "requires";
	String XDC_GEN_TOKEN_placeholder = "placeholder";
	String XDC_GEN_TOKEN_childPanels = "childPanels";
	String XDC_GEN_TOKEN_description = "description";
	String XDC_GEN_TOKEN_comment = "comment";
	String XDC_GEN_TOKEN_store = "store";
	String XDC_GEN_TOKEN_source = "source";
	String XDC_GEN_TOKEN_target = "target";
	String XDC_GEN_TOKEN_processor = "processor";
	String XDC_GEN_TOKEN_editor = "editor";
	String XDC_GEN_TOKEN_agent = "agent";
	String XDC_GEN_TOKEN_flags = "flags";
	String XDC_GEN_TOKEN_row = "row";
	String XDC_GEN_TOKEN_col = "col";
	String XDC_GEN_TOKEN_start = "start";

	String XDC_GEN_TOKEN_method = "method";
	String XDC_GEN_TOKEN_sum = "sum";

	String XDC_UTILS_MVEL_mvelCondition = "xdc_mvelCondition";
	String XDC_UTILS_MVEL_mvelText = "xdc_mvelText";
	String XDC_UTILS_MVEL_mvelType = "xdc_mvelType";
	String XDC_UTILS_MVEL_mvelTypeValidation = "Validation";
	String XDC_UTILS_MVEL_mvelTypeCalculation = "Calculation";
	String XDC_UTILS_MVEL_mvelTypeScript = "Script";
	String XDC_UTILS_MVEL_mvelTypeTemplate = "Template";
	String XDC_UTILS_MVEL_mvelCompObj = "xdc_mvelCompObj";
	String XDC_UTILS_MVEL_mvelCompCond = "xdc_mvelCompCond";

	String XDC_EXT_TOKEN_id = "id";
	String XDC_EXT_TOKEN_class = "class";
	String XDC_EXT_TOKEN_type = "type";
	String XDC_EXT_TOKEN_value = "value";
	String XDC_EXT_TOKEN_name = "name";
	String XDC_EXT_TOKEN_order = "order";
	String XDC_EXT_TOKEN_identifier = "identifier";
	String XDC_EXT_TOKEN_scheme = "scheme";
	String XDC_EXT_TOKEN_lang = "lang";
	String XDC_EXT_TOKEN_startDate = "startDate";
	String XDC_EXT_TOKEN_endDate = "endDate";

	String XDC_EXT_TOKEN_uri = "uri";
	String XDC_EXT_TOKEN_root = "root";
	String XDC_EXT_TOKEN_node = "node";

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
	String XDC_REPORT_TOKEN_units = "units";

	String XDC_REPORT_TOKEN_urlPackage = "urlPackage";
	String XDC_REPORT_TOKEN_urlJson = "urlJson";
	String XDC_REPORT_TOKEN_urlReport = "urlReport";
	String XDC_REPORT_TOKEN_sourceUrl = "sourceUrl";
	String XDC_REPORT_TOKEN_sourceAtts = "sourceAtts";

	String XDC_REPORT_TOKEN_packageStatus = "packageStatus";
	String XDC_REPORT_TOKEN_localPath = "localPath";
	String XDC_REPORT_TOKEN_localFilingPath = "localFilingPath";
	String XDC_REPORT_TOKEN_localMetaInfPath = "localMetaInfPath";

	String XDC_FACT_TOKEN_scenario = "scenario";
	String XDC_FACT_TOKEN_context = "context";

	String XDC_FACT_TOKEN_unit = "unit";
	String XDC_FACT_TOKEN_divide = "divide";
	String XDC_FACT_TOKEN_unitNumerator = "unitNumerator";
	String XDC_FACT_TOKEN_unitDenominator = "unitDenominator";
	String XDC_FACT_TOKEN_measure = "measure";

	String XDC_FACT_TOKEN_period = "period";
	String XDC_FACT_TOKEN_instant = "instant";

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

//@formatter:off  
	String[] UNIT_FIELDS = {XDC_FACT_TOKEN_unit, XDC_FACT_TOKEN_unitNumerator, XDC_FACT_TOKEN_unitDenominator, XDC_FACT_TOKEN_measure};
	String[] CONTEXT_FIELDS = {XDC_FACT_TOKEN_context, XDC_FACT_TOKEN_entity, XDC_FACT_TOKEN_instant, XDC_EXT_TOKEN_startDate, XDC_EXT_TOKEN_endDate, XDC_FACT_TOKEN_dimensions};
	Object[] FACT_DATA_FIELDS = {
			XDC_FACT_TOKEN_entity, XDC_FACT_TOKEN_context, XDC_FACT_TOKEN_concept, XDC_EXT_TOKEN_value, XDC_FACT_TOKEN_unit,
			
			XDC_FACT_TOKEN_instant, XDC_EXT_TOKEN_startDate, XDC_EXT_TOKEN_endDate, XDC_FACT_TOKEN_dimensions,
			XDC_FACT_TOKEN_unitNumerator, XDC_FACT_TOKEN_unitDenominator, XDC_FACT_TOKEN_measure,
			XDC_FACT_TOKEN_format, XDC_FACT_TOKEN_decimals, XDC_FACT_TOKEN_scale, XDC_FACT_TOKEN_sign,
			XDC_FACT_TOKEN_language, 
			
			XDC_FACT_TOKEN_xbrldockFactType, XDC_FACT_TOKEN_xbrldockOrigValue, XDC_FACT_TOKEN_xbrldockParseError, XDC_GEN_TOKEN_comment
	};
	Object[] FACT_TEXT_FIELDS = {
			XDC_FACT_TOKEN_entity, XDC_FACT_TOKEN_context, XDC_FACT_TOKEN_concept, XDC_EXT_TOKEN_value, 
			
			XDC_FACT_TOKEN_instant, XDC_EXT_TOKEN_startDate, XDC_EXT_TOKEN_endDate, XDC_FACT_TOKEN_dimensions,
			XDC_FACT_TOKEN_language, 
			
			XDC_FACT_TOKEN_xbrldockFactType, XDC_FACT_TOKEN_xbrldockOrigValue, XDC_FACT_TOKEN_xbrldockParseError, XDC_GEN_TOKEN_comment
	};
//@formatter:on 

}
