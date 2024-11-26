package com.xbrldock.poc.conn;

import java.io.File;
import java.io.FileFilter;

import com.xbrldock.format.XbrlDockFormatConsts;
import com.xbrldock.format.XbrlDockFormatUtils;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.poc.report.XbrlDockReportConsts;

public interface XbrlDockConnConsts extends XbrlDockPocConsts, XbrlDockFormatConsts, XbrlDockReportConsts {
	
	FileFilter XBRL_FILTER = new FileFilter() {
		@Override
		public boolean accept(File f) {
			return XbrlDockFormatUtils.canBeXbrl(f);
		}
	};

}
