package hu.sze.uni.xbrl;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustHandles;
import hu.sze.milab.dust.dev.DustDevUtils;
import hu.sze.milab.dust.stream.xml.DustXmlConsts;
import hu.sze.milab.dust.utils.DustUtilsConsts;

public interface XbrlConsts extends DustHandles, DustUtilsConsts, DustXmlConsts {
	String AUTHOR_XBRLDOCK = "XBRLDock";

	MindHandle XBRLDOCK_UNIT = Dust.lookup(AUTHOR_XBRLDOCK + DUST_SEP_ID + "0");
	MindHandle XBRLTEST_UNIT = Dust.lookup(AUTHOR_XBRLDOCK + DUST_SEP_ID + "1");

	MindHandle XBRLDOCK_NAR_XMLLOADER = DustDevUtils.newHandle(XBRLDOCK_UNIT, MIND_ASP_NARRATIVE, "XML Loader narrative");
	MindHandle XBRLDOCK_ATT_XMLLOADER_ROWDATA = DustDevUtils.newHandle(XBRLDOCK_UNIT, MIND_ASP_ATTRIBUTE, "XML Loader target data");
	MindHandle XBRLDOCK_ATT_XMLLOADER_ROWTEXT = DustDevUtils.newHandle(XBRLDOCK_UNIT, MIND_ASP_ATTRIBUTE, "XML Loader target text");

	MindHandle XBRLDOCK_NAR_STATS = DustDevUtils.newHandle(XBRLDOCK_UNIT, MIND_ASP_ATTRIBUTE, "XBRL stat narrative");

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
		File, EntityId, FactIdx, StartDate, EndDate, Instant, Dimensions, TagNamespace, TagId, Type, Format
	};

	enum FactFldData {
		Unit, OrigValue, Sign, Dec, Scale, Value, Error
	};

	enum FactFldText {
		Language, Value
	};

	enum FactType {
		Numeric, String, Text
	};

}
