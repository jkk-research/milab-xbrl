package com.xbrldock.utils;

import com.itextpdf.text.pdf.PdfReader;

public class XbrlDockUtilsPdf implements XbrlDockUtilsConsts {

//	public static FDFDocument readFile(String fileName) throws Exception {
//		return readFile(new File(fileName));
//	}
//
//	public static FDFDocument readFile(File f) throws Exception {
//		FDFDocument ret = null;
//
//		if (f.isFile()) {
//			ret = Loader.loadFDF(f);
//		}
//
//		return ret;
//	}

	public static int getPageCount(String fileName) throws Exception {
		PdfReader reader = new PdfReader(fileName);
		int p = reader.getNumberOfPages();
//		reader.close();
		return p;
	}


}
