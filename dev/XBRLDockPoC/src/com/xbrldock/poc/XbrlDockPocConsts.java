package com.xbrldock.poc;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.utils.XbrlDockUtilsConsts;

@SuppressWarnings("rawtypes")
public interface XbrlDockPocConsts extends XbrlDockConsts, XbrlDockUtilsConsts {	
	String XDC_CFGTOKEN_AGENT_metaManager = "metaManager";
	String XDC_CFGTOKEN_AGENT_esefConn = "esefConn";
	String XDC_CFGTOKEN_AGENT_manualReports = "manualReports";
	String XDC_CFGTOKEN_AGENT_gui = "gui";
	
	String XDC_CMD_GEN_GETCATALOG = "getCatalog";
	String XDC_CMD_METAMGR_GETMC = "getMC";
	String XDC_CMD_METAMGR_IMPORT = "import";
	String XDC_CMD_METAMGR_LOADMC = "loadMC";

	String XDC_CMD_GEN_SELECT = "select";

	String XDC_METAINFO_dir = "xdc_metaInfoDir";
	String XDC_METAINFO_urlRewrite = "xdc_urlRewrite";
	String XDC_METAINFO_pkgInfo = "xdc_pkgInfo";
	String XDC_METAINFO_entryPoints = "xdc_entryPoints";
	String XDC_METAINFO_entryPointRefs = "xdc_entryPointRefs";
	String XDC_METAINFO_ownedUrls = "xdc_ownedUrls";

	String XDC_METATOKEN_content = "xdc_content";
	
	String XDC_METATOKEN_items = "items";
	String XDC_METATOKEN_links = "links";
	String XDC_METATOKEN_references = "references";
	String XDC_METATOKEN_refLinks = "refLinks";
	String XDC_METATOKEN_labels = "labels";
	String XDC_METATOKEN_includes = "includes";
	String XDC_METATOKEN_url = "xdc_url";
	String XDC_METATOKEN_tagName = "xdc_tagName";
	

	String XDC_CFGTOKEN_dirStore = "dirStore";
	String XDC_CFGTOKEN_dirInput = "dirInput";

	String XDC_CFG_GEOM_location = "location";
	String XDC_CFG_GEOM_dimension = "dimension";
	String XDC_CFG_GEOM_x = "x";
	String XDC_CFG_GEOM_y = "y";

	
	String XDC_FNAME_METAINF = "META-INF";
	String XDC_FNAME_REPORTS = "reports";
	
	String XDC_FNAME_METACATALOG = "metaCatalog.json";
	
	String XDC_FNAME_CATALOG = "catalog.xml";
	String XDC_FNAME_TAXPACK = "taxonomyPackage.xml";

	
	public interface XDModSourceConnector extends GenAgent {
		int refresh(Collection<String> updated) throws Exception;
		Map getReportData(String id, Map target) throws Exception;
		void visitReports(GenProcessor<Map> visitor, GenProcessor<Map> filter) throws Exception;
		File getReportFile(String id, Object...path);
	}
	
	interface ReportDataHandler {
		void beginReport(String repId);
		void addNamespace(String ref, String id);
		void addTaxonomy(String tx);
		String processSegment(String segment, Map<String, Object> data);
		void endReport();
	}	
	
	interface ReportFormatHandler {
		void loadReport(InputStream in, ReportDataHandler dataHandler) throws Exception;
	}
}
