package hu.sze.uni.xbrl.edgar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.parser.JSONParser;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.dev.DustDevUtils;
import hu.sze.milab.dust.net.DustNetUtils;
import hu.sze.milab.dust.utils.DustUtils;

public class XbrlEdgarUtils implements XbrlEdgarConsts {

	private static Set<String> HEADERS = null;

	private static Long tsLastDownload = 0L;
	private static Object dlLock = new Object();

	public static synchronized boolean safeDownload(String url, File file) throws Exception {
		long ts = System.currentTimeMillis();
		long diff = ts - tsLastDownload;

		if (200 > diff) {
			synchronized (dlLock) {
				dlLock.wait(diff);
			}
		}

		tsLastDownload = System.currentTimeMillis();
		boolean success = false;
		try (FileOutputStream os = new FileOutputStream(file)) {
			if (null == HEADERS) {
				HEADERS = new HashSet<>();
				HEADERS.add(EDGAR_APIHDR_USER);
				HEADERS.add(EDGAR_APIHDR_ENCODING);
			}
			success = DustNetUtils.download(url, os, HEADERS, 1000);
		} catch (Exception e) {
			Dust.log(EVENT_TAG_TYPE_EXCEPTIONSWALLOWED, url, e);
		}

		if (!success && file.exists()) {
			file.delete();
		}

		return success;
	}

	public static File getFiling(File fReportRoot, String cik, String accn, String doc, Collection<String> reqPf)
			throws Exception {
		String urlRoot = EDGAR_URL_DATA + cik + "/" + accn.replace("-", "") + "/";
		String docName = "index.json";

		String dirName = DustUtils.getHash2(accn, File.separator) + File.separator + accn;
		File dir = new File(fReportRoot, dirName);
		dir.mkdirs();

		File fJson = new File(dir, docName);
		File fLink = null;

		if (!fJson.isFile() || (0 == fJson.length())) {
			String url = urlRoot + docName;
			safeDownload(url, fJson);
		}

		if (fJson.isFile()) {
			docName = selectFileFromJsonIndex(fJson, accn, doc, reqPf);
			String pf = DustUtils.getPostfix(docName, ".");
			String repName = accn + "." + pf;

			String urlRep = urlRoot + docName;

			File fRep = new File(dir, docName);
			fLink = new File(dir, repName);

			if (!fRep.isFile() || (0 == fRep.length())) {
				if (!DustUtils.isEqual(doc, docName)) {
					Dust.log(EVENT_TAG_TYPE_WARNING, "Doc name mismatch", cik, dirName, DustUtils.isEmpty(doc) ? "---" : doc,
							docName, urlRep);
				}

				Dust.log(EVENT_TAG_TYPE_INFO, "Downloading", cik, accn, DustUtils.isEmpty(doc) ? "---" : doc, urlRep);

				if (fLink.exists()) {
					fLink.delete();
				}

				if (safeDownload(urlRep, fRep)) {
					if (!DustUtils.isEqual(docName, repName)) {
						Files.createSymbolicLink(fLink.toPath(), fRep.toPath());
					}
				}
			} else {
//				Dust.log(EVENT_TAG_TYPE_INFO, "Resolving from cache", cik, accn, DustUtils.isEmpty(doc) ? "---" : doc, urlRep);				
			}
		}

		return fLink;
	}

	private static Pattern ptSkipFile = Pattern.compile("(R(\\d+)\\.xml)|(.*_(cal|def|lab|pre)\\.xml)");

	private static JSONParser parser = new JSONParser();

	private static String selectFileFromJsonIndex(File f, String accn, String doc, Collection<String> reqPf)
			throws Exception {
		String selName = null;

		String prefName = null;
		try (Reader fr = new FileReader(f)) {
			Object root = parser.parse(fr);
			Collection<Map<String, Object>> items = DustUtils.simpleGet(root, "directory", "item");

			if (null != items) {
				long maxLen = 0;

				for (Map<String, Object> item : items) {
					String name = (String) item.get("name");
					String pf = DustUtils.getPostfix(name, ".").toLowerCase();
					if ((null == reqPf) || reqPf.contains(pf)) {
						if (DustUtils.cutPostfix(name, ".").toLowerCase().equals(accn)) {
							prefName = name;
						}

						String size = (String) item.get("size");
						if (!DustUtils.isEmpty(size)) {
							Matcher m = ptSkipFile.matcher(name);
							if (!m.matches()) {
								int l = Integer.parseInt(size);
								if (l > maxLen) {
									maxLen = l;
									selName = name;
								}
							}
						} else if ((0 == maxLen) && name.toLowerCase().contains(accn.toLowerCase())) {
							selName = name;
						}
					}
				}
			}

			if (DustUtils.isEmpty(selName)) {
				if (DustUtils.isEmpty(prefName)) {
					DustDevUtils.breakpoint();
				} else {
					selName = prefName;
				}
			}
		}
		return selName;
	}

}
