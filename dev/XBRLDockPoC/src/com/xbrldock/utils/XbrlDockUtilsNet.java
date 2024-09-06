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

public class XbrlDockUtilsNet {
	public static void download(String url, File file, String... headers) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

		for (String h : headers) {
			int s = h.indexOf(":");
			String key = h.substring(0, s).trim();
			String val = h.substring(s + 1).trim();
			conn.setRequestProperty(key, val);
		}

		InputStream is = conn.getInputStream();

		if ( "gzip".equals(conn.getContentEncoding()) ) {
			try (GZIPInputStream i = new GZIPInputStream(is)) {
				OutputStream o = Files.newOutputStream(file.toPath());
				IOUtils.copy(i, o);
			}
		} else {
			try (BufferedInputStream in = new BufferedInputStream(is); FileOutputStream fileOutputStream = new FileOutputStream(file)) {
				byte dataBuffer[] = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
					fileOutputStream.write(dataBuffer, 0, bytesRead);
				}
			}
		}
	}

}
