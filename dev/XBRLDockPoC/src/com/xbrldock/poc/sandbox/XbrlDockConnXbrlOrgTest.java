package com.xbrldock.poc.sandbox;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.xbrldock.XbrlDock;
import com.xbrldock.dev.XbrlDockDevCounter;
import com.xbrldock.dev.XbrlDockDevMonitor;
import com.xbrldock.dev.XbrlDockDevReportDump;
import com.xbrldock.poc.conn.XbrlDockConnUtils;
import com.xbrldock.poc.conn.xbrlorg.XbrlDockConnXbrlOrgConsts;
import com.xbrldock.poc.report.XbrlDockReportLoader;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.stream.XbrlDockStreamJson;

@SuppressWarnings({ "rawtypes" })
public class XbrlDockConnXbrlOrgTest implements XbrlDockConnXbrlOrgConsts {
	
	public static void exportHun(File root, Map<String, Map> filings) throws IOException {
		File target = new File("work/hunzips");
		XbrlDockUtilsFile.ensureDir(target);

		for (Map.Entry<String, Map> fe : filings.entrySet()) {
			String id = fe.getKey();
			Map filingData = fe.getValue();

			String country = XbrlDockUtils.simpleGet(filingData, XDC_REPORT_TOKEN_sourceAtts, "country");

			if ("HU".equalsIgnoreCase(country)) {
				String lp = XbrlDockUtils.simpleGet(filingData, "localPath");
				File f = new File(root, lp);

				if (f.isDirectory()) {
					File[] zips = f.listFiles(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.endsWith(XDC_FEXT_ZIP);
						}
					});

					if ((null != zips) && (0 < zips.length)) {
						File fZip = zips[0];
						String zipName = fZip.getName();
						Files.copy(fZip.toPath(), new File(target, zipName).toPath(), StandardCopyOption.REPLACE_EXISTING);

						System.out.println(XbrlDockUtils.sbAppend(null, "\t", true, id, zipName, filingData.get("startDate"), filingData.get("endDate"), filingData.get("entityName")));
						
					}
				}
			}
		}
	}

	public static void collectStats(File dirStore, File dirInput, Map<String, Map> filings) throws Exception {
		File fRoot = new File(dirInput, XDC_FNAME_CONNFILINGS);

		XbrlDockDevMonitor mon = new XbrlDockDevMonitor("Statistics", 100);
		Set<String> msgs = new TreeSet<>();

		XbrlDockDevReportDump coll = new XbrlDockDevReportDump();
		coll.logAll = false;
		XbrlDockDevCounter stats = new XbrlDockDevCounter("Stats", true);

		for (Map.Entry<String, Map> fe : filings.entrySet()) {
			mon.step();

			Map filingData = fe.getValue();

			File dataDir = XbrlDockConnUtils.getFilingDir(dirStore, filingData, true, false);
			File ff;
			
			if (( ff = new File(dataDir, XDC_FNAME_REPDATA)).isFile()) {
				stats.add("File Count - Data");
				stats.add("File Size  - Data", ff.length());
				
				coll.reset();
				XbrlDockReportLoader.readCsv(ff, filingData, coll);
				
				for ( String s : XDC_SEGMENTS ) {
					stats.add("Segment count - " + s, coll.getCount(s));
				}
			}

			if (( ff = new File(dataDir, XDC_FNAME_REPTEXT)).isFile()) {
				stats.add("File Count - Text");
				stats.add("File Size  - Text", ff.length());
			}

			File dir = XbrlDockConnUtils.getFilingDir(fRoot, filingData, true, false);
			if (dir.isDirectory()) {
				File rep = XbrlDockConnUtils.findReportInFilingDir(dir, msgs);

				if ((null != rep) && rep.isFile()) {
					String ext = XbrlDockUtils.getPostfix(rep.getName().toUpperCase(), ".");
					long fl = rep.length();
					stats.add("File Count - Input Report");
					stats.add("File Count - Input Report - " + ext);
					stats.add("File Size  - Input Report", fl);
					stats.add("File Size  - Input Report - " + ext, fl);
				}
			}
		}

		XbrlDock.log(EventLevel.Trace, "Stats complete", mon.getCount(), stats);
	}
	public static void test() throws Exception {
		Map<String, Map<String, Object>> reports = XbrlDockStreamJson.readJson("temp/allReports.json");

		Map<String, String> repErr = new TreeMap<>();
		Map<String, String> zipErr = new TreeMap<>();

		XbrlDock.log(EventLevel.Info, "Report count", reports.size());

		int uaCount = 0;
		int esefCount = 0;

		for (Map.Entry<String, Map<String, Object>> er : reports.entrySet()) {
			String repId = er.getKey();

			if (repId.contains("UAIFRS")) {
				++uaCount;
				continue;
			}

			++esefCount;

			Map<String, Object> ri = er.getValue();

			String rep = (String) ri.get("report_url");
			String zip = (String) ri.get("package_url");

			if (XbrlDockUtils.isEmpty(rep)) {
				repErr.put(repId, "MISSING");
			} else if (!rep.endsWith("html")) {
				repErr.put(repId, rep);
			}

			if (XbrlDockUtils.isEmpty(zip)) {
				zipErr.put(repId, "MISSING");
			} else if (!zip.endsWith(".zip")) {
				zipErr.put(repId, zip);
			}
		}

		XbrlDock.log(EventLevel.Info, "ESEF", esefCount, "UAIFRS", uaCount, "repErrCount", repErr.size(), "zipErrCount",
				zipErr.size());

		XbrlDock.log(EventLevel.Info, "Report not xhtml", repErr);
		XbrlDock.log(EventLevel.Info, "Package question", zipErr);

	}
}
