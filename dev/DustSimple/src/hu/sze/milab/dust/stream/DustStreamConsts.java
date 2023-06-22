package hu.sze.milab.dust.stream;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustMetaConsts;

public interface DustStreamConsts extends DustMetaConsts {
	
	enum XmlData {
		Element, Attribute, Content, 
	}

	public static MindHandle STREAM_UNIT = Dust.createHandle();
	
	public static MindHandle STREAM_ASP_STREAM = Dust.createHandle();
	public static MindHandle STREAM_ATT_STREAM_FILE = Dust.createHandle();

}
