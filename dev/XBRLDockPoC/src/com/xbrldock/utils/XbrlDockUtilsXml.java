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
	
	private static EntityResolver DEF_ENTITY_RESOLVER;
	
	public static void setDefEntityResolver(EntityResolver er) {
		DEF_ENTITY_RESOLVER = er;
	}
	
	private static ThreadLocal<DocumentBuilder> tdb = new ThreadLocal<DocumentBuilder>() {
		protected DocumentBuilder initialValue() {
			try {
				return dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				return XbrlDockException.wrap(e);
			}
		};
	};
	
	
	public static Document parseDoc(InputStream is) throws Exception {
		DocumentBuilder db = tdb.get();
		db.reset();
		
		db.setEntityResolver(DEF_ENTITY_RESOLVER);
		
		return db.parse(is);
	}
	
	public static Document parseDoc(File f) throws Exception {
		try ( FileInputStream fis = new FileInputStream(f) ) {
			return parseDoc(fis);
		}
	}
	
}
