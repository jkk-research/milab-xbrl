package hu.sze.uni.xbrl;

import java.io.File;

import hu.sze.milab.dust.DustConsts;

public interface XbrlConsts extends DustConsts {
	String POSTFIX_TXT = "_Txt.csv";
	String POSTFIX_VAL = "_Val.csv";

	int TEXT_CUT_AT = 200;
	
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
		Json, Zip, ContentVal, ContentTxt, GenJson //, Package
	}

	enum XbrlReportFormat {
		XHTML(false), HTML(false), XBRL(true), JSON(false), XML(true) /* ?? */,;

		public final boolean isXml;

		private XbrlReportFormat(boolean isXml) {
			this.isXml = isXml;
		}

		public static XbrlReportFormat getFormat(File f) {
			XbrlReportFormat ret = null;

			if ( f.exists() ) {
				String fn = f.getName().toUpperCase();
				int d = fn.lastIndexOf('.');
				String type = fn.substring(d + 1).toUpperCase();

				try {
					ret = XbrlReportFormat.valueOf(type);
				} catch (Throwable e) {
				}
			}

			return ret;
		}
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
