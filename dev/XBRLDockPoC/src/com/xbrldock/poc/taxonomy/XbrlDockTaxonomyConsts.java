package com.xbrldock.poc.taxonomy;

import com.xbrldock.poc.XbrlDockPocConsts;

public interface XbrlDockTaxonomyConsts extends XbrlDockPocConsts {
	
	enum TaxonomyKeys {
		items, links, references, refLinks
	}
	
	String KEY_LOADED = "loaded";
	
	String TAXONOMY_FNAME = "taxonomy" + XBRLDOCK_EXT_JSON;
	String RES_FNAME_POSTFIX = "_res" + XBRLDOCK_EXT_JSON;
	
	interface TaxonomyReader {
		
	}

}
