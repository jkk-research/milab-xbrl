package com.xbrldock.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;

import com.xbrldock.XbrlDockException;

public class XbrlDockUtilsXml implements XbrlDockUtilsConsts {
	
	private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	
	private static ThreadLocal<DocumentBuilder> tdb = new ThreadLocal<DocumentBuilder>() {
		protected DocumentBuilder initialValue() {
			try {
				return dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				return XbrlDockException.wrap(e);
			}
		};
	};
	
	
	public static Document parseDoc(InputStream is, EntityResolver er) throws Exception {
		DocumentBuilder db = tdb.get();
		db.reset();
		
		db.setEntityResolver(er);
		
		return db.parse(is);
	}
	
	public static Document parseDoc(File f, EntityResolver er) throws Exception {
		try ( FileInputStream fis = new FileInputStream(f) ) {
			return parseDoc(fis, er);
		}
	}
	
}
