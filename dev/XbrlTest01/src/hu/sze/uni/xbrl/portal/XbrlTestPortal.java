package hu.sze.uni.xbrl.portal;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import java.io.FileFilter;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.uni.http.DustHttpServerJetty;
import hu.sze.uni.xbrl.XbrlFilingManager;
import hu.sze.uni.xbrl.XbrlReportLoaderDomBase;
import hu.sze.uni.xbrl.XbrlUtils;

@SuppressWarnings("rawtypes")
public class XbrlTestPortal implements XbrlTestPortalConsts {

	private File dataRoot;
	private XbrlFilingManager filings;

	private long spaceToFree;

	public XbrlTestPortal() {
		dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
	}

	void init() throws Exception {

		filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);

		getAllZips();

//		initJetty();

	}

	void getAllZips() throws Exception {
		boolean extract = true;
		
		spaceToFree = 0;
		int errCount = 0;
		Map<String, Map> reportData = filings.getReportData();

		int count = 0;

		FileFilter ffIsDir = new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}
		};

//		File dir = new File("work");

		for (Map.Entry<String, Map> e : reportData.entrySet()) {
			String id = e.getKey();
			Map repSrc = e.getValue();

			if ( 0 == (++count % 100) ) {
//				System.out.println("Count " + count);
			}

			String pkgUrl = XbrlUtils.access(repSrc, AccessCmd.Peek, null, "package_url");
			String repUrl = XbrlUtils.access(repSrc, AccessCmd.Peek, null, "report_url");

			String repDirName = XbrlUtils.access(repSrc, AccessCmd.Peek, null, XbrlFilingManager.LOCAL_DIR);
			File repDir = new File(filings.getRepoRoot(), repDirName);

			File repFile = new File(repDir, "extractedReport.xhtml");
			boolean repFileExists = repFile.isFile();

			try {
				if ( repDir.isDirectory() ) {
					File csvVal = new File(repDir, "Report_Val.csv");
					if ( csvVal.isFile() ) {

						if ( repFileExists ) {
							removeRepFile(repFile);
						}

						continue;
					}
				} else {
					repDir.mkdirs();
				}

				if ( extract ) {

					if ( !repFileExists ) {
						File repZip = filings.getReport(repSrc, XbrlReportType.Zip);

						if ( DustUtils.isEmpty(repUrl) ) {
							if ( 0 == repDir.listFiles(ffIsDir).length ) {
								File unzipDir = new File(repDir, DustUtils.cutPostfix(repZip.getName(), "."));
								XbrlTestPortalUtils.extractWithApacheZipFile(null, repZip, unzipDir);
							}

							File rf = filings.findReportToLoad(repDir);

							if ( null != rf ) {
								Files.copy(rf.toPath(), repFile.toPath());
								repFileExists = true;
							} else {
								Dust.dumpObs(id, pkgUrl, "Missing report url (and could not guess)", repDir.getCanonicalPath());
								continue;
							}
						} else {
							int sep = pkgUrl.lastIndexOf("/");
							String repName = repUrl.substring(sep + 1);

							XbrlTestPortalUtils.extractWithApacheZipFile(repName, repZip, repFile);
							repFileExists = repFile.exists();
						}
					}
				}

				if ( repFileExists ) {
					XbrlReportLoaderDomBase.createSplitCsv(repFile, repDir, "Report", 200);
					removeRepFile(repFile);
				}
			} catch (Throwable t) {
				Dust.dumpObs(id, pkgUrl, t);
				t.printStackTrace();
				++errCount;
			}
		}

		Dust.dumpObs("Total count", reportData.size(), "errors", errCount, "space to free", spaceToFree);
	}

	public void removeRepFile(File repFile) {
		Dust.dumpObs("Delete", repFile);
		spaceToFree += repFile.length();
		repFile.delete();
	}

	void initJetty() throws Exception {

		DustHttpServerJetty srv = new DustHttpServerJetty() {
			@Override
			protected void initHandlers() {
				super.initHandlers();

				addServlet("/list/*", new XbrlTestServletReportList(filings));
				addServlet("/report/*", new XbrlTestServletReportData(filings));
				addServlet("/bin/*", new XbrlTestServletReportBinary(filings));
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
