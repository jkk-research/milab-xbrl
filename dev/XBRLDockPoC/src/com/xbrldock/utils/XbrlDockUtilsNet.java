package com.xbrldock.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;

public class XbrlDockUtilsNet implements XbrlDockUtilsConsts {

	private static long lastRequest;
	private static long waitMsec = 500;

	private static Object LOCK = new Object();
	
	private static File CACHE_ROOT;
	private static ThreadLocal<Map<String, File>> URL_REWRITES = new ThreadLocal<Map<String, File>>() {
		@Override
		protected Map<String, File> initialValue() {
			return new TreeMap<String, File>(Collections.reverseOrder());
		}
	};

	public static void setCacheRoot(String root) {
		CACHE_ROOT = new File(root);
	}
	
	public static File getCacheRoot() {
		return CACHE_ROOT;
	}

	public static HttpURLConnection connect(String url, String... headers) throws Exception {
		long now = System.currentTimeMillis();

		long diff = lastRequest - now + waitMsec;
		if (0 < diff) {
			synchronized (LOCK) {
				try {
					LOCK.wait(diff);
				} catch (Throwable t) {
					XbrlDockException.swallow(t);
				}
			}
		}
		
		lastRequest = System.currentTimeMillis();

//		url = URLEncoder.encode(url, XDC_CHARSET_UTF8).replace("+", "%20");
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

		for (String h : headers) {
			int s = h.indexOf(":");
			String key = h.substring(0, s).trim();
			String val = h.substring(s + 1).trim();
			conn.setRequestProperty(key, val);
		}

		return conn;
	}
	
	public static void setRewrite(File root, Map<String, String> prefixes) throws Exception {
		Map<String, File> ur = URL_REWRITES.get();

		ur.clear();

		for (Map.Entry<String, String> ep : prefixes.entrySet()) {
			ur.put(ep.getKey(), new File(root, ep.getValue()).getCanonicalFile());
		}
	}

	public static InputSource resolveEntity(String url) throws SAXException, IOException {
		return new InputSource(resolveEntityStream(url));
	}

	public static InputStream resolveEntityStream(String url) throws SAXException, IOException {
		String path = XbrlDockUtils.getPostfix(url, "://");
		File fCache = new File(CACHE_ROOT, path);

		Set<Entry<String, File>> rewriteEntries = URL_REWRITES.get().entrySet();
		
		for (Map.Entry<String, File> ep : rewriteEntries) {
			String p = ep.getKey();
			if (url.startsWith(p)) {
				fCache = new File(ep.getValue(), url.substring(p.length()));
				break;
			}
		}

		if (!fCache.isFile()) {
			XbrlDockUtilsFile.ensureDir(fCache.getParentFile());
			try {
				XbrlDockUtilsNet.download(url, fCache);
			} catch (Exception e) {
				XbrlDockException.wrap(e, "Downloading", url);
			}
		}

		if (fCache.isFile()) {
			return new FileInputStream(fCache);
		}

		return null;
	}

	public static void download(String url, File file, String... headers) throws Exception {
		XbrlDock.log(EventLevel.Info, "Downloading url", url, "to file", file.getCanonicalPath());

		HttpURLConnection conn = connect(url, headers);
		
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(10000);
		
		String redirect = conn.getHeaderField("Location");
		if (redirect != null){
			conn = connect(redirect, headers); 
		}
		
		InputStream is = conn.getInputStream();

		if ("gzip".equals(conn.getContentEncoding())) {
			try (GZIPInputStream i = new GZIPInputStream(is)) {
				OutputStream o = Files.newOutputStream(file.toPath());
				IOUtils.copy(i, o);
			}
		} else {
			try (BufferedInputStream in = new BufferedInputStream(is);
					FileOutputStream fileOutputStream = new FileOutputStream(file)) {
				byte dataBuffer[] = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
					fileOutputStream.write(dataBuffer, 0, bytesRead);
				}
			}
		}
	}

}
