package com.xbrldock.poc.format;

import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtilsConsts;

public interface XbrlDockFormatConsts extends XbrlDockPocConsts, XbrlDockUtilsConsts {
	String XBRL_PERIOD_DATEFORMATSTR = "yyyy-MM-dd'T'HH:mm:ss";

//@formatter:off
	enum JsonKeys {
		 documentInfo, documentType, features, namespaces,
		 
		 taxonomy, 
		 
		 facts, value, decimals, dimensions, concept, entity, period, unit
	}
	
//@formatter:on
}
