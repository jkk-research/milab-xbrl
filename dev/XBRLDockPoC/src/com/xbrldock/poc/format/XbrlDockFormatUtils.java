package com.xbrldock.poc.format;

import java.io.File;

import com.xbrldock.utils.XbrlDockUtils;

public class XbrlDockFormatUtils implements XbrlDockFormatConsts {
	
	public static boolean canBeXbrl(File f) {
		return f.isFile() && canBeXbrl(f.getName());
	}

	public static boolean canBeXbrl(String fileName) {
		String pf = "." + XbrlDockUtils.getPostfix(fileName, ".");
		
		switch ( pf ) {
		case XBRLDOCK_EXT_XHTML:
		case XBRLDOCK_EXT_XML:
		case XBRLDOCK_EXT_XBRL:
		case XBRLDOCK_EXT_HTML:
			return true;
		}
		
		return false;
	}

}
