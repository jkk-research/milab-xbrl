package hu.sze.uni.xbrl;

import hu.sze.milab.dust.DustHandles;
import hu.sze.milab.dust.stream.xml.DustXmlConsts;
import hu.sze.milab.dust.utils.DustUtilsConsts;

public interface XbrlConsts extends DustHandles, XbrlHandles, DustUtilsConsts, DustXmlConsts {

	String XBRLTOKEN_SCENARIO = "xbrli:scenario";
	String XBRLTOKEN_CONTEXT = "xbrli:context";
	String XBRLTOKEN_UNIT = "xbrli:unit";
	String XBRLTOKEN_UNIT_NUM = "xbrli:unitNumerator";
	String XBRLTOKEN_UNIT_DENOM = "xbrli:unitDenominator";
	String XBRLTOKEN_MEASURE = "xbrli:measure";
	
	String XBRLTOKEN_CONTINUATION = "ix:continuation";
	
	String XBRLTOKEN_DIMENSION = "dimension";
	
	String XBRLTOKEN_ = "";
	
	int STRING_LIMIT = 100;
	

	enum FactFldCommon {
		File, EntityId, CtxId, FactId, StartDate, EndDate, Instant, Dimensions, TagNamespace, TagId, Type, Format
	};

	enum FactFldData {
		UnitId, Unit, OrigValue, Sign, Dec, Scale, Value, Error
	};

	enum FactFldText {
		Language, Value
	};

	enum FactType {
		Numeric, String, Text
	};

}
