package com.xbrldock.poc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.utils.XbrlDockUtilsConsts;

@SuppressWarnings("rawtypes")
public interface XbrlDockPocConsts extends XbrlDockConsts, XbrlDockUtilsConsts {	
	String XDC_CFGTOKEN_MOD_urlCache = "urlCache";
	String XDC_CFGTOKEN_MOD_taxmgr = "taxmgr";
	String XDC_CFGTOKEN_MOD_esefConn = "esefConn";
	String XDC_CFGTOKEN_MOD_manualReports = "manualReports";
	String XDC_CFGTOKEN_MOD_gui = "gui";
	
	String XDC_CFGTOKEN_dirStore = "dirStore";
	String XDC_CFGTOKEN_dirInput = "dirInput";

	String XDC_CFG_GEOM_location = "location";
	String XDC_CFG_GEOM_dimension = "dimension";
	String XDC_CFG_GEOM_x = "x";
	String XDC_CFG_GEOM_y = "y";

	
	String XDC_FNAME_METAINF = "META-INF";
	String XDC_FNAME_REPORTS = "reports";
	
	String XDC_FNAME_CATALOG = "catalog.xml";
	String XDC_FNAME_TAXPACK = "taxonomyPackage.xml";

	public interface XDModUrlResolver extends GenModule, EntityResolver {
		File getCacheRoot();
		void setRewrite(File root, Map<String, String> prefixes) throws Exception;
		
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException;
		public InputStream resolveEntityStream(String publicId, String systemId) throws SAXException, IOException;
	}
	
	public interface XDModTaxonomyManager extends GenModule {
		void importTaxonomy(File taxSource) throws Exception;
	}
	
	public interface XDModSourceConnector extends GenModule {
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
