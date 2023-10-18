package hu.sze.milab.xbrl;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustMetaConsts;
import hu.sze.milab.dust.stream.xml.DustStreamXmlConsts;

public interface XbrlConsts extends DustMetaConsts, DustStreamXmlConsts {
	
	String PREFIX_XBRL = "hu.sze.milab.xbrl.";
	
	String TYPE_XBRL_ENTITIES = "xbrl:entities";
	String TYPE_XBRL_REPORTS = "xbrl:reports";
	String TYPE_XBRL_CONTEXTS = "xbrl:contexts";
	String TYPE_XBRL_FACTS = "xbrl:facts";



	enum XbrlInfoType {
		Context, Unit, Fact
	}
	
	enum XbrlFactDataType {
		number, string, text, date, bool, empty
	}
	
	enum XbrlFactDataInfo {
		OrigValue, Unit, Format, Sign, Dec, Scale, Type, Value, Err
	}
	
	public static MindHandle XBRL_UNIT = Dust.resolveID(null, null);
	
	public static MindHandle XBRL_ASP_REPORT = Dust.resolveID(null, null);
	public static MindHandle XBRL_ATT_REPORT_CONTEXTS = Dust.resolveID(null, null);


}
