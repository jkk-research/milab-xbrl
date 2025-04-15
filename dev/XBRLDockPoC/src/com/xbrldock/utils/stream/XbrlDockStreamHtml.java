package com.xbrldock.utils.stream;

import java.io.File;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class XbrlDockStreamHtml implements XbrlDockStreamConsts {

	public static Document readHtml(String fileName) throws Exception {
		return readHtml(new File(fileName));
	}

	public static Document readHtml(File f) throws Exception {
		Document ret = null;

		if (f.isFile()) {
			ret = Jsoup.parse(f);
		}

		return ret;
	}

}
