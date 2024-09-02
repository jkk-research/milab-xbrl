package com.xbrldock.utils;

import java.io.File;
import java.io.FileReader;

import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")
public class XbrlDockUtilsJson implements XbrlDockUtilsConsts {
	public static <RetType> RetType readJson(String fileName) throws Exception {
		Object ret = null;
		File f = new File(fileName);

		if (f.isFile()) {
			try (FileReader r = new FileReader(f)) {
				JSONParser p = new JSONParser();
				ret = p.parse(r);
			}
		}

		return (RetType) ret;
	}
}
