package hu.sze.uni.xbrl;

public interface XbrlConsts {
	enum ContainerType {
		Arr, Map,
	}

	enum AccessCmd {
		Peek, Set,
	}

	enum XbrlInfoType {
		Context, Unit, Fact
	}

	enum XbrlReportType {
		Json, Package, 
	}
	
	enum XbrlReportFormat {
		XHTML, XBRL, JSON, XML
	}

	String XML_ELEMENT = "XmlElement";
	String XML_ATTRIBUTES = "XmlAttributes";
	String XML_CONTENT = "XmlContent";
	
	String KEY_XBRL_ID = "id";
	String KEY_XBRL_CTXREF = "contextRef";
	String KEY_XBRL_UNITREF = "unitRef";

	String KEY_XBRL_ENTITY = "entity";
	String KEY_XBRL_STARTDATE = "startDate";
	String KEY_XBRL_ENDDATE = "endDate";
	String KEY_XBRL_INSTANT = "instant";
	
	String KEY_XBRL_UNIT_MEASURE = "measure";
	String KEY_XBRL_UNIT_NUMERATOR = "unitNumerator";
	String KEY_XBRL_UNIT_DENOMINATOR = "unitDenominator";

	String XBRL_SOURCE_FILINGS = "xbrl.org";
	String XBRL_ENTITYID_LEI = "lei";

	String IDSEP = ":";

}
