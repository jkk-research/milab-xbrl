package com.xbrldock.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")
public class XbrlDockUtilsJson implements XbrlDockUtilsConsts {

	public static <RetType> RetType readJson(String fileName) throws Exception {
		return readJson(new File(fileName));
	}

	public static <RetType> RetType readJson(File f) throws Exception {
		Object ret = null;

		if (f.isFile()) {
			try (FileReader r = new FileReader(f)) {
				JSONParser p = new JSONParser();
				ret = p.parse(r);
			}
		}

		return (RetType) ret;
	}

	public static void writeJson(String fileName, Object ob) throws Exception {
		writeJson(new File(fileName), ob);
	}

	public static void writeJson(File f, Object ob) throws Exception {
		try (FileWriter w = new FileWriter(f)) {
			JSONValue.writeJSONString(ob, w);
		}
	}

}
