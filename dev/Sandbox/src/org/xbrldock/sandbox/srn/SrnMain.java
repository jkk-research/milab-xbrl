package org.xbrldock.sandbox.srn;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.xbrldock.XbrlDock;
import com.xbrldock.dev.XbrlDockDevMonitor;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsNet;
import com.xbrldock.utils.stream.XbrlDockStreamCsv;
import com.xbrldock.utils.stream.XbrlDockStreamJson;
import com.xbrldock.utils.stream.XbrlDockStreamPdf;
import com.xbrldock.utils.stream.XbrlDockStreamUtils;

public class SrnMain extends XbrlDock implements SrnConsts {

	Map<String, Object> appParams;
//	Map<Object, Map<String, Object>> docs = new TreeMap<>();

	ArrayList<Map<String, Object>> log = new ArrayList<>();
	boolean doDownload = true;

	String mode;

	@Override
	public Object process(String cmd, Map<String, Object> params) throws Exception {
		String fName;

		switch (cmd) {
		case XDC_CMD_GEN_Init:
			this.appParams = params;
			mode = XbrlDockUtils.simpleGet(params, "mode");
			break;
		case XDC_CMD_GEN_Begin:
//			fName = XbrlDockUtils.simpleGet(appParams, XDC_GEN_TOKEN_store);
//			ArrayList<Map<String, Object>> docArr = XbrlDockUtilsJson.readJson(fName);
//
//			for (Map<String, Object> di : docArr) {
//				docs.put(di.get("id"), di);
//			}

			fName = XbrlDockUtils.simpleGet(appParams, "params", mode, XDC_GEN_TOKEN_source);
			File f = new File(fName);

			fName = XbrlDockUtils.simpleGet(appParams, "params", mode, XDC_GEN_TOKEN_target);
			File dir = new File(fName);
			XbrlDockUtilsFile.ensureDir(dir);
			
			File logJson = new File(dir, "log.json");
			if ( logJson.isFile() ) {
				File logCsv = new File(dir, "log.csv");
				XbrlDock.log(EventLevel.Info, "Only converting log to csv", logCsv.getCanonicalPath());
				XbrlDockStreamUtils.json2Csv(logJson, logCsv, ",");
				return null;
			}

			XbrlDockUtilsNet.disableCertificateValidation();

			XbrlDockDevMonitor m = new XbrlDockDevMonitor("Reading", 100);

			GenAgent rowProc = new GenAgent() {
				@Override
				public Object process(String cmd, Map<String, Object> params) throws Exception {
					switch (cmd) {
					case XDC_CMD_GEN_Process:
						String key = null;
						String href = null;
						boolean allowCache = false;

						switch (mode) {
						case "1":
							key = XbrlDockUtils.buildKey(params, "_", "", "country", "isin", "year", "type", "document_id");
							href = XbrlDockUtils.simpleGet(params, "href");
							allowCache = "url_cached".equals(params.get("source"));

							break;
						case "2":
							href = XbrlDockUtils.simpleGet(params, "link");
							if ( XbrlDockUtils.isEmpty(href)) {
								return null;
							}
							key = UUID.nameUUIDFromBytes(href.getBytes()).toString();
							break;
						}

						m.step();
						TreeMap<String, Object> tm = new TreeMap<>(params);
						log.add(tm);
						
						tm.remove("");

						String ext;
						ext = XbrlDockUtils.cutPostfix(href, "?");
						ext = XbrlDockUtils.getPostfix(ext, ".");

						if (XbrlDockUtils.isEmpty(ext) || (5 < ext.length())) {
							ext = "pdf";
						}

						String fName = key + "." + ext;
						File report = new File(dir, fName);
						tm.put("fileName", fName);

						if (report.isFile()) {
							XbrlDock.log(EventLevel.Info, "Report exists in cache", report.getCanonicalPath());

							if (optCheckFile(ext, report, tm)) {
								break;
							} else {
								allowCache = false;
							}
						}

						if (allowCache) {
							href = "https://api.sustainabilityreportingnavigator.com/api/documents/" + XbrlDockUtils.simpleGet(params, "document_id") + "/download";
						}

						if (!href.toLowerCase().startsWith("http")) {
							tm.put(SBX_SRN_ERROR, "Invalid href");
						} else {
							if (doDownload) {
								try {
									XbrlDockUtilsNet.download(href, report,
											"user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
											"sec-ch-ua-platform: \"macOS\"", "sec-ch-ua: \"Google Chrome\";v=\"135\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"",
											"accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
											"accept-encoding: gzip, deflate, br, zstd", "accept-language: en-US,en-GB;q=0.9,en;q=0.8,hu;q=0.7");

									optCheckFile(ext, report, tm);

								} catch (Throwable t) {
									tm.put(SBX_SRN_ERROR, t.toString());
									XbrlDock.log(EventLevel.Error, tm);
								}
							} else {
								XbrlDock.log(EventLevel.Warning, "Skip download", fName);
							}
						}

						break;
					}
					return null;
				}
			};

			XbrlDockStreamCsv.readCsv(f, rowProc, ";");

			XbrlDockStreamJson.writeJson(logJson, log);

			break;
		}
		return null;

	}

	protected boolean optCheckFile(String ext, File report, TreeMap<String, Object> tm) {
		tm.put("fileSize", report.length());

		try {
			switch (ext) {
			case "pdf":
				tm.put("pdfPageCount", XbrlDockStreamPdf.getPageCount(report.getCanonicalPath()));
				break;
			}
		} catch (Throwable e) {
			String errMsg = e.toString();
			if ( errMsg.contains("Unknown encryption type R = 6")) {
				return true;
			}
			tm.put(SBX_SRN_ERROR, ext + " file read error " + errMsg);
			return false;
		}

		return true;
	}

}
