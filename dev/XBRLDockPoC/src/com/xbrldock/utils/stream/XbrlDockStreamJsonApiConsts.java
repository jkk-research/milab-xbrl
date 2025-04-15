package com.xbrldock.utils.stream;

public interface XbrlDockStreamJsonApiConsts extends XbrlDockStreamConsts {

	String JSONCONST_NULL = "null";
	String JSONCONST_TRUE = "true";
	String JSONCONST_FALSE = "false";

// https://jsonapi.org/format/
	String JSONAPI_VERSION = "1.1";

	String XDC_JSONAPI_ = "";

	String XDC_JSONAPI_TOKEN_jsonapi = "jsonapi";
	String XDC_JSONAPI_TOKEN_version = "version";
	String XDC_JSONAPI_TOKEN_ext = "ext";
	String XDC_JSONAPI_TOKEN_profile = "profile";

	String XDC_JSONAPI_TOKEN_meta = "meta";
	String XDC_JSONAPI_TOKEN_links = "links";
	String XDC_JSONAPI_TOKEN_type = "type";
	String XDC_JSONAPI_TOKEN_describedby = "describedby";

	String XDC_JSONAPI_TOKEN_data = "data";
	String XDC_JSONAPI_TOKEN_errors = "errors";
	String XDC_JSONAPI_TOKEN_included = "included";

	String XDC_JSONAPI_TOKEN_id = "id";
	String XDC_JSONAPI_TOKEN_lid = "lid";
	String XDC_JSONAPI_TOKEN_attributes = "attributes";
	String XDC_JSONAPI_TOKEN_relationships = "relationships";

	String XDC_JSONAPI_TOKEN_self = "self";
	String XDC_JSONAPI_TOKEN_related = "related";
	String XDC_JSONAPI_TOKEN_href = "href";
	String XDC_JSONAPI_TOKEN_rel = "rel";
	String XDC_JSONAPI_TOKEN_title = "title";
	String XDC_JSONAPI_TOKEN_hreflang = "hreflang";
	String XDC_JSONAPI_TOKEN_first = "first";
	String XDC_JSONAPI_TOKEN_last = "last";
	String XDC_JSONAPI_TOKEN_prev = "prev";
	String XDC_JSONAPI_TOKEN_next = "next";

	String XDC_JSONAPI_TOKEN_detail = "detail";
	String XDC_JSONAPI_TOKEN_count = "count";

	
	String XDC_JSONAPI_PARAM_include = "include";
	String XDC_JSONAPI_PARAM_fields = "fields";
	String XDC_JSONAPI_PARAM_sort = "sort";
	String XDC_JSONAPI_PARAM_filter = "filter";
	String XDC_JSONAPI_PARAM_page = "page";
	String XDC_JSONAPI_PARAM_limit = "limit";
	String XDC_JSONAPI_PARAM_offset = "offset";

	
	String XDC_JSONAPI_FILTER_equals = "equals";
	String XDC_JSONAPI_FILTER_lessThan = "lessThan";
	String XDC_JSONAPI_FILTER_lessOrEqual = "lessOrEqual";
	String XDC_JSONAPI_FILTER_greaterThan = "greaterThan";
	String XDC_JSONAPI_FILTER_greaterOrEqual = "greaterOrEqual";
	
	String XDC_JSONAPI_FILTER_contains = "contains";
	String XDC_JSONAPI_FILTER_startsWith = "startsWith";
	String XDC_JSONAPI_FILTER_endsWith = "endsWith";
	
	String XDC_JSONAPI_FILTER_isType = "isType";
	String XDC_JSONAPI_FILTER_count = "count";
	String XDC_JSONAPI_FILTER_any = "any";
	String XDC_JSONAPI_FILTER_has = "has";
	
	String XDC_JSONAPI_FILTER_not = "not";
	String XDC_JSONAPI_FILTER_or = "or";
	String XDC_JSONAPI_FILTER_and = "and";

}
