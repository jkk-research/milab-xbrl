package hu.sze.uni.xbrl;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustHandles;
import hu.sze.milab.dust.dev.DustDevUtils;
import hu.sze.milab.dust.utils.DustUtilsConsts;

public interface XbrlConsts extends DustHandles, DustUtilsConsts {
	String AUTHOR_XBRLDOCK = "XBRLDock";
	
	MindHandle XBRLDOCK_UNIT = Dust.lookup(AUTHOR_XBRLDOCK + DUST_SEP_ID + "Core");
	MindHandle XBRLTEST_UNIT = Dust.lookup(AUTHOR_XBRLDOCK + DUST_SEP_ID + "XbrlTest");
	
	MindHandle XBRLDOCK_NAR_XMLLOADER = DustDevUtils.newHandle(XBRLDOCK_UNIT, MIND_ASP_NARRATIVE, "XML Loader narrative");
	MindHandle XBRLDOCK_ATT_XMLLOADER_ROWDATA = DustDevUtils.newHandle(XBRLDOCK_UNIT, MIND_ASP_ATTRIBUTE, "XML Loader target data");
	MindHandle XBRLDOCK_ATT_XMLLOADER_ROWTEXT = DustDevUtils.newHandle(XBRLDOCK_UNIT, MIND_ASP_ATTRIBUTE, "XML Loader target text");

}
