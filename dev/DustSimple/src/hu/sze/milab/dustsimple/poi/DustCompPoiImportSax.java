package com.gollywolly.dustcomp.dust.io.poi;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.gollywolly.dustcomp.DustCompServices.DustUtilsProcessor;

public class DustCompPoiImportSax implements DustUtilsProcessor, DustCompPoiComponents {
	Set<String> readCols;

	Set<String> sheetNames;

	private static class SheetHandler extends DefaultHandler {
		private SharedStringsTable sst;
		private String lastContents;
		private boolean nextIsString;

		private SheetHandler(SharedStringsTable sst) {
			this.sst = sst;
		}

		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			// c => cell
			if (name.equals("c")) {
				// Print the cell reference
				System.out.print(attributes.getValue("r") + " - ");
				// Figure out if the value is an index in the SST
				String cellType = attributes.getValue("t");
				if (cellType != null && cellType.equals("s")) {
					nextIsString = true;
				} else {
					nextIsString = false;
				}
			}
			// Clear contents cache
			lastContents = "";
		}

		public void endElement(String uri, String localName, String name) throws SAXException {
			// Process the last contents as required.
			// Do now, as characters() may be called more than once
			if (nextIsString) {
				int idx = Integer.parseInt(lastContents);
				lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
				nextIsString = false;
			}

			// v => contents of a cell
			// Output after we've seen the string contents
			if (name.equals("v")) {
				System.out.println(lastContents);
			}
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			lastContents += new String(ch, start, length);
		}
	}

	@Override
	public void dust_utils_processor_process() throws Exception {
		evaluate("1463562117990_vizsga2015_0506.xlsx");
	}

	public void evaluate(String fileName) throws Exception {

		OPCPackage opcp = OPCPackage.open(fileName);

		for (PackagePart pp : opcp.getParts()) {
			String s = pp.getPartName().getName();

			System.out.println(s);
		}

		XSSFReader r = new XSSFReader(opcp);
		SharedStringsTable sst = r.getSharedStringsTable();

		ContentHandler handler = new SheetHandler(sst);

		XMLReader reader = XMLReaderFactory.createXMLReader();
		reader.setContentHandler(handler);

		Iterator<InputStream> sheets = r.getSheetsData();
		while (sheets.hasNext()) {
			System.out.println("Processing new sheet:\n");
			InputStream sheet = sheets.next();
			InputSource sheetSource = new InputSource(sheet);
			reader.parse(sheetSource);
			sheet.close();
			System.out.println("");
		}

	}

}
