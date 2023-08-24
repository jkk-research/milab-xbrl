package hu.sze.uni.xbrl.portal;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.uni.http.DustHttpServerJetty;
import hu.sze.uni.xbrl.XbrlFilingManager;
import hu.sze.uni.xbrl.XbrlReportLoaderDomBase;
import hu.sze.uni.xbrl.XbrlUtils;

@SuppressWarnings("rawtypes")
public class XbrlTestPortal implements XbrlTestPortalConsts {

	private File dataRoot;
	XbrlFilingManager filings;

//	ArrayList<String[]> allFacts = new ArrayList<>();
//	ArrayList<Map> allFacts = new ArrayList<>(); TOO BIG!

	private long spaceToFree;

	public XbrlTestPortal() {
		dataRoot = new File(System.getProperty("user.home") + "/work/xbrl/data");
	}

	void init() throws Exception {

		filings = new XbrlFilingManager(dataRoot, true);
		filings.setDownloadOnly(false);

		getAllZips();

		initJetty();

	}

//	@SuppressWarnings("unchecked")
	void getAllZips() throws Exception {
		boolean extract = true;

		spaceToFree = 0;
		int errCount = 0;
		Map<String, Map> reportData = filings.getReportData();

		int count = 0;

		int parsedRepCount = 0;
		int totalValCount = 0;
		int totalTxtCount = 0;
		ArrayList<String> errFactLines = new ArrayList<>();

		@SuppressWarnings("unused")
		Set<File> csvUpdate = new HashSet<>();

		FileFilter ffIsDir = new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}
		};
//		SimpleDateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd");

		for (Map.Entry<String, Map> e : reportData.entrySet()) {
			String id = e.getKey();
			Map repSrc = e.getValue();

			if ( 0 == (++count % 100) ) {
				System.out.println("Count " + count);
			}

			String pkgUrl = XbrlUtils.access(repSrc, AccessCmd.Peek, null, "package_url");
			String repUrl = XbrlUtils.access(repSrc, AccessCmd.Peek, null, "report_url");

			String repDirName = XbrlUtils.access(repSrc, AccessCmd.Peek, null, XbrlFilingManager.LOCAL_DIR);
			File repDir = new File(filings.getRepoRoot(), repDirName);

			File repFile = new File(repDir, "extractedReport.xhtml");
			File csvVal = new File(repDir, "Report_Val.csv");
			boolean repFileExists = repFile.isFile();

			try {
				if ( !csvVal.isFile() ) {
					if ( !repDir.isDirectory() ) {
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
					}
				}

				if ( repFile.isFile() ) {
					Dust.dumpObs("Delete", repFile);
					spaceToFree += repFile.length();
					repFile.delete();
				}

				if ( csvVal.isFile() ) {
					filings.optLoadFacts(id);
//					ArrayList<String[]> allFacts = new ArrayList<>();
//					allFactsByRep.put(id, allFacts);
//
//					++parsedRepCount;
////					String fPref = csvVal.getName() + "\t";
//					try (BufferedReader br = new BufferedReader(new FileReader(csvVal))) {
//						DustUtils.TableReader tr = null;
//
//						for (String line; (line = br.readLine()) != null;) {
//							if ( !DustUtils.isEmpty(line) ) {
//								String[] data = line.split("\t");
//
//								if ( null == tr ) {
//									tr = contentReaders.get(line);
//									if ( null == tr ) {
//										tr = new DustUtils.TableReader(data);
//										contentReaders.put(line, tr);
//									}
//									headers.put(id, tr);
//								} else {
//									String strVal = tr.get(data, "Value");
//									if ( null == strVal ) {
//										Dust.dumpObs("seems to be: fixed-empty text in val?", line);
////										csvUpdate.add(csvVal);
//										continue;
//									}
//									if ( strVal.startsWith("Txt len") ) {
//										totalTxtCount++;
//									} else {
//										totalValCount++;
////										String err = tr.get(data, "Err");
////										if ( DustUtils.isEmpty(err) ) 
//										{
//											allFacts.add(data);
////											Map fact = tr.get(data, null, "Unit", "Format", "Value");
////											tr.getUntil(data, fact, "OrigValue");
////											fact.put("repId", id);
////											
////											allFacts.add(fact);
////										} else {
//////											if ( !err.contains("monthname") ) 
////											{
////												errFactLines.add(fPref + line);
//////												csvUpdate.add(csvVal);
////											}
//										}
//									}
//								}
//							}
//						}
//					}
				}
			} catch (Throwable t) {
				Dust.dumpObs(id, pkgUrl, t);
				t.printStackTrace();
				++errCount;
			}
		}

		Dust.dumpObs("Total count", reportData.size(), "errors", errCount, "space to free", spaceToFree);

		for (String e : errFactLines) {
			Dust.dumpObs(e);
		}

		Dust.dumpObs("Parsed files", parsedRepCount, "Val", totalValCount, "Txt", totalTxtCount, "Err", errFactLines.size());

//		for ( File f : csvUpdate ) {
//			f.delete();
//		}
	}

	void initJetty() throws Exception {

		DustHttpServerJetty srv = new DustHttpServerJetty() {
			@Override
			protected void initHandlers() {
				super.initHandlers();

				addServlet("/list/*", new XbrlTestServletReportList(XbrlTestPortal.this));
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
