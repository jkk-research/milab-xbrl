package hu.sze.uni.xbrl.portal;

import java.io.File;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.net.httpsrv.DustHttpServletDirectFile;
import hu.sze.milab.dust.stream.DustStreamUrlCache;
import hu.sze.milab.xbrl.XbrlCoreUtils;
import hu.sze.milab.xbrl.test.XbrlTaxonomyLoader;
import hu.sze.uni.http.DustHttpServerJetty;
import hu.sze.uni.xbrl.XbrlFilingManager;

public class XbrlTestPortal implements XbrlTestPortalConsts {

	private File dataRoot;
	XbrlFilingManager filings;

	XbrlTaxonomyLoader taxonomyCollector;


	public XbrlTestPortal() {
		dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
	}

	void init() throws Exception {

		filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);
		
		filings.loadAllData();
		
		DustStreamUrlCache urlCache = new DustStreamUrlCache(new File(dataRoot, "urlCache"), false);
		File taxonomyRoot = new File(dataRoot, "taxonomies");
		File fRoot = new File(taxonomyRoot, "IFRSAT-2023-03-23");
		taxonomyCollector = XbrlCoreUtils.readTaxonomy(urlCache, fRoot, "ifrs-full");
		taxonomyCollector.collectData();						
		
		initJetty();
	}

	void initJetty() throws Exception {

		DustHttpServerJetty srv = new DustHttpServerJetty() {
			@Override
			protected void initHandlers() {
				super.initHandlers();

				addServlet("/list/*", new XbrlTestServletReportList(XbrlTestPortal.this));
				addServlet("/report/*", new XbrlTestServletReportData(filings));
				addServlet("/bin/*", new XbrlTestServletReportBinary(filings));
				
				addServlet("/*", new DustHttpServletDirectFile("webroot"));
			}
		};
		srv.activeInit();
	}

	public static void main(String[] args) throws Exception {
		Dust.main(args);

		XbrlTestPortal portal = new XbrlTestPortal();

		portal.init();
	}

}
