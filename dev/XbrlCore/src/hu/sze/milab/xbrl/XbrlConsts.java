package hu.sze.milab.xbrl;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustMetaConsts;
import hu.sze.milab.dust.stream.DustStreamConsts;

public interface XbrlConsts extends DustMetaConsts, DustStreamConsts {

	enum XbrlInfoType {
		Context, Unit, Fact
	}
	
	public static MindHandle XBRL_UNIT = Dust.createHandle();
	
	public static MindHandle XBRL_ASP_REPORT = Dust.createHandle();
	public static MindHandle XBRL_ATT_REPORT_CONTEXTS = Dust.createHandle();


}
