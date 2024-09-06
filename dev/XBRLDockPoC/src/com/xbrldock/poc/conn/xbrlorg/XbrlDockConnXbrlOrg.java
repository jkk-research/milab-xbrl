package com.xbrldock.poc.conn.xbrlorg;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDockException;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsJson;

@SuppressWarnings({"rawtypes", "unchecked"})
public class XbrlDockConnXbrlOrg implements XbrlDockConnXbrlOrgConsts {
	String sourceName = "xbrl.org";
	
	File dataRoot;

	Map catalog;

	public XbrlDockConnXbrlOrg(String rootPath) throws Exception {
		dataRoot = new File(rootPath);

		if (!dataRoot.exists()) {
			dataRoot.mkdirs();
		}

		catalog = XbrlDockUtilsJson.readJson(new File(dataRoot, PATH_CATALOG));

		if (null == catalog) {
			catalog = new TreeMap();
		}
	}

	public void test() throws Exception {
//		XbrlDockConnXbrlOrgTest.test();
		refresh();
	}

	public void refresh() throws Exception {
		Map resp = XbrlDockUtilsJson.readJson(new File(dataRoot, PATH_SRVRESP));

		Map entities = XbrlDockUtils.safeGet(catalog, CatalogKeys.entities, MAP_CREATOR);
		Map langs = XbrlDockUtils.safeGet(catalog, CatalogKeys.languages, MAP_CREATOR);

		Map atts;
		String id;
		Map<String, String> locLang = new TreeMap<>();
		Map<String, Map> locEnt = new TreeMap<>();
		Map<EntityKeys, Object> entityData;

		Collection<Map<String, Object>> c;

		c = XbrlDockUtils.simpleGet(resp, JsonApiKeys.included);
		for (Map<String, Object> i : c) {
			id = XbrlDockUtils.simpleGet(i, JsonApiKeys.id);
			atts = XbrlDockUtils.simpleGet(i, JsonApiKeys.attributes);
			ResponseType rt = XbrlDockUtils.simpleGetEnum(ResponseType.class, i, JsonApiKeys.type);

			switch (rt) {
			case entity:
				String eid = EntityIdType.lei + XBRLDOCK_SEP_ID + XbrlDockUtils.simpleGet(atts, ResponseKeys.identifier);
				entityData = XbrlDockUtils.safeGet(entities, eid, MAP_CREATOR);
				entityData.put(EntityKeys.id, eid);
				entityData.put(EntityKeys.name, XbrlDockUtils.simpleGet(atts, ResponseKeys.name));
				entityData.put(EntityKeys.urlSource, XbrlDockUtils.simpleGet(i, JsonApiKeys.links, JsonApiKeys.self));
				locEnt.put(id, entityData);
				
				break;
			case language:
				String code = XbrlDockUtils.simpleGet(atts, ResponseKeys.code);
				langs.put(code, XbrlDockUtils.simpleGet(atts, ResponseKeys.name));
				locLang.put(id, code);
				break;
			default:
				XbrlDockException.wrap(null, "Should not be here");
				break;
			}
		}

		Map filings = XbrlDockUtils.safeGet(catalog, CatalogKeys.filings, MAP_CREATOR);

		c = XbrlDockUtils.simpleGet(resp, JsonApiKeys.data);
		for (Map<String, Object> i : c) {
			atts = XbrlDockUtils.simpleGet(i, JsonApiKeys.attributes);			
			ResponseType rt = XbrlDockUtils.simpleGetEnum(ResponseType.class, i, JsonApiKeys.type);

			switch (rt) {
			case filing:
				id = XbrlDockUtils.simpleGet(atts, ResponseKeys.fxo_id);
				Map<ReportKeys, Object> filingData = XbrlDockUtils.safeGet(filings, id, MAP_CREATOR);

				filingData.put(ReportKeys.source, sourceName);
				filingData.put(ReportKeys.id, id);
				filingData.put(ReportKeys.periodEnd, XbrlDockUtils.simpleGet(atts, ResponseKeys.period_end));
				filingData.put(ReportKeys.published, XbrlDockUtils.simpleGet(atts, ResponseKeys.date_added));
				filingData.put(ReportKeys.urlPackage, XbrlDockUtils.simpleGet(atts, ResponseKeys.package_url));
				filingData.put(ReportKeys.sourceUrl, XbrlDockUtils.simpleGet(i, JsonApiKeys.links, JsonApiKeys.self));
				filingData.put(ReportKeys.sourceAtts, atts);
				
				String linkId = XbrlDockUtils.simpleGet(i, JsonApiKeys.relationships, ResponseType.language, JsonApiKeys.data, JsonApiKeys.id);
				filingData.put(ReportKeys.langCode, locLang.get(linkId));

				linkId = XbrlDockUtils.simpleGet(i, JsonApiKeys.relationships, ResponseType.entity, JsonApiKeys.data, JsonApiKeys.id);
				entityData = locEnt.get(linkId);
				filingData.put(ReportKeys.entityId, entityData.get(EntityKeys.id));
				filingData.put(ReportKeys.entityName, entityData.get(EntityKeys.name));

				break;
			default:
				XbrlDockException.wrap(null, "Should not be here");
				break;
			}
		}

		XbrlDockUtilsJson.writeJson(new File(dataRoot, PATH_CATALOG), catalog);
	}
}
