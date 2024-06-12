package hu.sze.uni.xbrl.edgar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Reader;
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
	private static Set<String> PFX = null;

	private static Long tsLastDownload = 0L;
	private static Object dlLock = new Object();

	public static synchronized void safeDownload(String url, File file) throws Exception {
		long ts = System.currentTimeMillis();
		long diff = ts - tsLastDownload;

		if (200 > diff) {
			synchronized (dlLock) {
				dlLock.wait(diff);
			}
		}

		tsLastDownload = System.currentTimeMillis();
		try (FileOutputStream os = new FileOutputStream(file)) {
			if (null == HEADERS) {
				HEADERS = new HashSet<>();
				HEADERS.add(EDGAR_APIHDR_USER);
				HEADERS.add(EDGAR_APIHDR_ENCODING);
			}
			DustNetUtils.download(url, os, HEADERS, 1000);
		} catch (Exception e) {
			Dust.log(EVENT_TAG_TYPE_EXCEPTIONSWALLOWED, url, e);
		}
	}

	public static File getFiling(File fReportRoot, String cik, String accn, String doc) throws Exception {
		String urlRoot = EDGAR_URL_DATA + cik + "/" + accn.replace("-", "") + "/";
		String docName = "index.json";

		String dirName = DustUtils.getHash2(accn, File.separator) + File.separator + accn;
		File dir = new File(fReportRoot, dirName);
		dir.mkdirs();

		File f = new File(dir, docName);

		if (!f.isFile() || (0 == f.length())) {
			String url = urlRoot + docName;
			safeDownload(url, f);
		}

		if (f.isFile()) {
			docName = selectFileFromJsonIndex(f, accn);
			String pf = DustUtils.getPostfix(docName, ".");
			f = new File(dir, accn + "." + pf);
			String urlRep = urlRoot + docName;

			if (!f.isFile() || (0 == f.length())) {
				if (!DustUtils.isEqual(doc, docName)) {
					Dust.log(EVENT_TAG_TYPE_WARNING, "Doc name mismatch", cik, dirName, DustUtils.isEmpty(doc) ? "---" : doc,
							docName, urlRep);
				}

				Dust.log(EVENT_TAG_TYPE_INFO, "Downloading", cik, accn, DustUtils.isEmpty(doc) ? "---" : doc, urlRep);

				safeDownload(urlRep, f);
			} else {
//				Dust.log(EVENT_TAG_TYPE_INFO, "Resolving from cache", cik, accn, DustUtils.isEmpty(doc) ? "---" : doc, urlRep);				
			}
		}

		return f;
	}

	private static Pattern ptSkipFile = Pattern.compile("(R(\\d+)\\.xml)|(.*_(cal|def|lab|pre)\\.xml)");

	private static JSONParser parser = new JSONParser();

	private static String selectFileFromJsonIndex(File f, String accn) throws Exception {
		String selName = null;

		if (null == PFX) {
			PFX = new HashSet<>();
			PFX.add("htm");
			PFX.add("html");
			PFX.add("xhtml");
			PFX.add("xml");
			PFX.add("xbrl");
			PFX.add("txt");
		}

		try (Reader fr = new FileReader(f)) {
			Object root = parser.parse(fr);
			Collection<Map<String, Object>> items = DustUtils.simpleGet(root, "directory", "item");

			if (null != items) {
				long maxLen = 0;

				for (Map<String, Object> item : items) {
					String name = (String) item.get("name");
					String size = (String) item.get("size");

					if (!DustUtils.isEmpty(size)) {
						String pf = DustUtils.getPostfix(name, ".").toLowerCase();
						if (PFX.contains(pf)) {
							Matcher m = ptSkipFile.matcher(name);
							if (!m.matches()) {
								int l = Integer.parseInt(size);
								if (l > maxLen) {
									maxLen = l;
									selName = name;
								}
							}
						}
					} else if ((0 == maxLen) && name.toLowerCase().contains(accn.toLowerCase())) {
						selName = name;
					}
				}
			}

			if (DustUtils.isEmpty(selName)) {
				DustDevUtils.breakpoint();
			}
		}
		return selName;
	}

}
