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
		case XDC_FEXT_XHTML:
		case XDC_FEXT_XML:
		case XDC_FEXT_XBRL:
		case XDC_FEXT_HTML:
			return true;
		}
		
		return false;
	}

}
