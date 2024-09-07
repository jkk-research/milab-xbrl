package com.xbrldock.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.utils.IOUtils;

import com.xbrldock.XbrlDockException;

public class XbrlDockUtilsNet implements XbrlDockUtilsConsts {

	private static long lastRequest;
	private static long waitMsec = 500;

	private static Object LOCK = new Object();

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

//		url = URLEncoder.encode(url, XBRLDOCK_CHARSET_UTF8).replace("+", "%20");
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

		for (String h : headers) {
			int s = h.indexOf(":");
			String key = h.substring(0, s).trim();
			String val = h.substring(s + 1).trim();
			conn.setRequestProperty(key, val);
		}

		return conn;
	}

	public static void download(String url, File file, String... headers) throws Exception {
		HttpURLConnection conn = connect(url, headers);
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
