package hu.sze.milab.xbrl;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustMetaConsts;
import hu.sze.milab.dust.stream.xml.DustStreamXmlConsts;

public interface XbrlConsts extends DustMetaConsts, DustStreamXmlConsts {
	
	String PREFIX_XBRL = "hu.sze.milab.xbrl.";


	enum XbrlInfoType {
		Context, Unit, Fact
	}
	
	public static MindHandle XBRL_UNIT = Dust.resolveID(null, null);
	
	public static MindHandle XBRL_ASP_REPORT = Dust.resolveID(null, null);
	public static MindHandle XBRL_ATT_REPORT_CONTEXTS = Dust.resolveID(null, null);


}
