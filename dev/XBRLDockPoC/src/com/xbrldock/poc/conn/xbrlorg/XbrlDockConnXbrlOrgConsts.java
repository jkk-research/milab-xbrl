package com.xbrldock.poc.conn.xbrlorg;

import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.poc.format.XbrlDockFormatConsts;
import com.xbrldock.utils.XbrlDockUtilsJsonApiConsts;

public interface XbrlDockConnXbrlOrgConsts extends XbrlDockPocConsts, XbrlDockFormatConsts, XbrlDockUtilsJsonApiConsts {

	String PATH_CATALOG = "catalog.json";
	String PATH_SRVRESP = "filings.org.response.json";
	
	String PATH_FILING_CACHE = "filings";
	String PATH_DATA = "data";
	
	enum CatalogKeys {
		entities, filings, languages
	}
	
	enum ResponseType {
		filing, entity, language
	}
	
	enum ResponseKeys {
		fxo_id, identifier, name, code, period_end, date_added, package_url, report_url, json_url
	}
	
	enum PackageStatus {
		reportIdentified, reportFoundSingle, reportFoundMulti, reportMisplaced, reportNotFound
	}


}
