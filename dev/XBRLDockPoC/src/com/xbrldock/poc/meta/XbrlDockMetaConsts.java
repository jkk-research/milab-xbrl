package com.xbrldock.poc.meta;

import com.xbrldock.poc.XbrlDockPocConsts;

public interface XbrlDockMetaConsts extends XbrlDockPocConsts {
	
	String KEY_LOADED = "loaded";
	
	String XDC_TAXONOMYHEAD_FNAME = "taxonomyHead" + XDC_FEXT_JSON;
	String XDC_TAXONOMYDATA_FNAME = "taxonomyData" + XDC_FEXT_JSON;
	String XDC_TAXONOMYREFS_FNAME = "taxonomyRefs" + XDC_FEXT_JSON;
	String XDC_TAXONOMYRES_FNAME_PREFIX = "taxonomyRes_";
	
	interface TaxonomyReader {
		
	}

}
