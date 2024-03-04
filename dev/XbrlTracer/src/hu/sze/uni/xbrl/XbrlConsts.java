package hu.sze.uni.xbrl;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustHandles;
import hu.sze.milab.dust.utils.DustUtilsConsts;

public interface XbrlConsts extends DustHandles, DustUtilsConsts {
	String AUTHOR_XBRLDOCK = "XBRLDock.hu";
	
	MindHandle XBRLTEST_UNIT = Dust.lookup(AUTHOR_XBRLDOCK + DUST_SEP_ID + "XbrlTest");

}
