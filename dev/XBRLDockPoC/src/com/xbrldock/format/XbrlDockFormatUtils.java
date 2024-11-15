package com.xbrldock.format;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.utils.XbrlDockUtils;

public class XbrlDockFormatUtils implements XbrlDockFormatConsts {
	
	public static int TXT_CLIP_LENGTH = 100;
	
	public static boolean canBeXbrl(File f) {
		return f.isFile() && canBeXbrl(f.getName());
	}

	public static boolean canBeXbrl(String fileName) {
		String pf = "." + XbrlDockUtils.getPostfix(fileName, ".");
		
		switch ( pf ) {
		case XDC_FEXT_XHTML:
		case XDC_FEXT_XML:
		case XDC_FEXT_XBRL:
		case XDC_FEXT_HTML:
			return true;
		}
		
		return false;
	}

	private static final Map<String, String> SEGMENT_IDS = new TreeMap<>();
	
	static {
		SEGMENT_IDS.put(XDC_REP_SEG_Unit, XDC_FACT_TOKEN_unit);
		SEGMENT_IDS.put(XDC_REP_SEG_Context, XDC_FACT_TOKEN_context);
		SEGMENT_IDS.put(XDC_REP_SEG_Fact, XDC_EXT_TOKEN_id);
	}
	
	public static String getSegmentIdKey(String seg) {
		return SEGMENT_IDS.get(seg);
	}

}
