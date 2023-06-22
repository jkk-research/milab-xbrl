package com.gollywolly.dustcomp.dust.io.poi;

import com.gollywolly.dustcomp.DustCompComponents.DustIdentifier;
import com.gollywolly.dustcomp.api.DustCompApiNames;
import com.gollywolly.dustcomp.api.DustCompApiUtils;

public interface DustCompPoiNames extends DustCompApiNames {
	DustIdentifier FLD_TYPETARGET = DustCompApiUtils.getPathBuilder().smartId("TypeTarget");
	DustIdentifier FLD_DATATARGET = DustCompApiUtils.getPathBuilder().smartId("DataTarget");
	
	DustIdentifier FLD_GEOCODER = DustCompApiUtils.getPathBuilder().smartId("Geocoder");
	
	DustIdentifier FLD_SAVER = DustCompApiUtils.getPathBuilder().smartId("Saver");
	DustIdentifier FLD_SAVESTREAM= DustCompApiUtils.getPathBuilder().smartId("SaveStream");
	
	DustIdentifier FLD_EXP_TYPEDEPTH = DustCompApiUtils.getPathBuilder().smartId("typeDepth");
	
	String CMD_CTRL = "!ERPortCtrl";
	String CMD_SKIP = "!skip";
	String CMD_SKIPALL = "!skipAll";
	String CMD_TOCOL = "!toCol";
	String CMD_ROLLCOL = "!rollCol";
	String CMD_CALCCOL = "!calcCol";
	String CMD_COLNAMES = "!colNames";
	String CMD_RESETCOL = "!resetCol";
	String CMD_GEOCODE = "!geocode";
	
	String ESCAPECHARS = "~!,:";
}
