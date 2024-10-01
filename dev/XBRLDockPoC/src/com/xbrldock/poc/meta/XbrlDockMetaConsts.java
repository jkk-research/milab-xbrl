package com.xbrldock.poc.meta;

import com.xbrldock.poc.XbrlDockPocConsts;

public interface XbrlDockMetaConsts extends XbrlDockPocConsts {
	
	String XDC_TAXONOMY_TOKEN_items = "items";
	String XDC_TAXONOMY_TOKEN_links = "links";
	String XDC_TAXONOMY_TOKEN_references = "references";
	String XDC_TAXONOMY_TOKEN_refLinks = "refLinks";

	String KEY_LOADED = "loaded";
	
	String XDC_TAXONOMY_FNAME = "taxonomy" + XDC_FEXT_JSON;
	String XDC_RES_FNAME_POSTFIX = "_res" + XDC_FEXT_JSON;
	
	interface TaxonomyReader {
		
	}

}
