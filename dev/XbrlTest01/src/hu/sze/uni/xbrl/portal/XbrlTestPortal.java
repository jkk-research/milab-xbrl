package hu.sze.uni.xbrl.portal;

import java.io.File;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.net.httpsrv.DustHttpServletDirectFile;
import hu.sze.uni.http.DustHttpServerJetty;
import hu.sze.uni.xbrl.XbrlFilingManager;

public class XbrlTestPortal implements XbrlTestPortalConsts {

	private File dataRoot;
	XbrlFilingManager filings;


	public XbrlTestPortal() {
		dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
	}

	void init() throws Exception {

		filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);
		
		filings.loadAllData();
		
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
