package com.xbrldock.poc.meta;

import com.xbrldock.poc.XbrlDockPocConsts;

public interface XbrlDockMetaConsts extends XbrlDockPocConsts {
	
	String XDC_METATOKEN_content = "xdc_content";
	
	String XDC_METATOKEN_items = "items";
	String XDC_METATOKEN_links = "links";
	String XDC_METATOKEN_references = "references";
	String XDC_METATOKEN_refLinks = "refLinks";
	String XDC_METATOKEN_includes = "includes";

	String KEY_LOADED = "loaded";
	
	String XDC_TAXONOMYHEAD_FNAME = "taxonomyHead" + XDC_FEXT_JSON;
	String XDC_TAXONOMYDATA_FNAME = "taxonomyData" + XDC_FEXT_JSON;
	String XDC_TAXONOMYRES_FNAME_PREFIX = "taxonomyRes_";
	
	interface TaxonomyReader {
		
	}

}
