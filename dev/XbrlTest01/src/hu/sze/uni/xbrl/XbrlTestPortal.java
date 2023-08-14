package hu.sze.uni.xbrl;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

import hu.sze.milab.dust.Dust;
import hu.sze.uni.http.DustHttpServerJetty;
import hu.sze.uni.http.DustHttpServlet;

@SuppressWarnings("rawtypes") 
public class XbrlTestPortal implements XbrlConsts {

	private File dataRoot;
	private XbrlFilingManager filings;
	
	private DustHttpServlet srvReportList = new DustHttpServlet() {
		private static final long serialVersionUID = 1L;

		@Override
		protected void processRequest(Map data) {
			Dust.dumpObs("get report list", data);
			
			PrintWriter out = Dust.access(data, MindAccess.Peek, null, ServletData.Writer);
			out.println("Here comes the report list " + data);
			
			Dust.access(data, MindAccess.Set, CONTENT_TEXT, ServletData.ContentType);
		}
	};
	
	private DustHttpServlet srvReport = new DustHttpServlet() {
		private static final long serialVersionUID = 1L;
		
		@Override
		protected void processRequest(Map data) {
			Dust.dumpObs("get report data", data);			
			
			PrintWriter out = Dust.access(data, MindAccess.Peek, null, ServletData.Writer);
			out.println("Here comes the report list " + data);
			Dust.access(data, MindAccess.Set, CONTENT_TEXT, ServletData.ContentType);
		}
	};
	

	public XbrlTestPortal() {
		dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
	}
	
	void init() throws Exception {

		filings = new XbrlFilingManager(dataRoot, true);
		filings.downloadOnly = false;

		
		DustHttpServerJetty srv = new DustHttpServerJetty() {
			@Override
			protected void initHandlers() {
				super.initHandlers();
				
				addServlet("/list/*", srvReportList);
				addServlet("/report/*", srvReport);
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
