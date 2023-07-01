package hu.sze.uni.xbrl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlTagCoverage implements XbrlConsts, XbrlListener {
	public static final String NAME = "name";
	public static final String FILINGS = "filings";
	public static final String TAGS = "tags";
	public static final String TAXONOMY = "taxonomy";
	public static final String META = ".";
	public static final String FORMAT = "format";
	public static final String CELL_ADDRESS = "__CellAddress";

	Map<String, Map> filings = new TreeMap<>();

	XbrlUtilsFactory<String, Map> namespaces = new XbrlUtilsFactory<String, Map>(true) {
		@Override
		protected Map create(String key, Object... hints) {
			Map ret = new HashMap();

			ret.put(NAME, key);
			ret.put(FORMAT, new TreeMap<String, String>());
			ret.put(FILINGS, new TreeSet<String>());
			ret.put(TAGS, new XbrlUtilsFactory.Simple(true, TreeSet.class));
			ret.put(TAXONOMY, new XbrlUtilsFactory.Simple(true, HashMap.class));

			return ret;
		}
	};

	public String currentFiling;

	@Override
	public void handleDocInfo(Map info) {
	}

	void setCurrentFiling(Map filing, File fRep) {
		XbrlUtils.access(filing, AccessCmd.Set, fRep, "attributes", XbrlFilingManager.REPORT_FILE);

		currentFiling = XbrlUtils.access(filing, AccessCmd.Peek, null, "attributes", "fxo_id");
		filings.put(currentFiling, filing);

		System.out.println(currentFiling + " : " + fRep.getAbsolutePath());
	}

	public XbrlUtilsCounter cntDimKeys = new XbrlUtilsCounter(true);

	@Override
	public void handleXbrlInfo(XbrlInfoType type, Map info) {
		if ( type == XbrlInfoType.Fact ) {
			for (Object k : ((Map) info.get("dimensions")).keySet()) {
				cntDimKeys.add(k);
			}
			String tag = XbrlUtils.access(info, AccessCmd.Peek, null, "dimensions", "concept");
			if ( null != tag ) {
				int sep = tag.indexOf(':');
				String ns = tag.substring(0, sep);
				String t = tag.substring(sep + 1);

				Map m = namespaces.get(ns);

				((Set) m.get(FILINGS)).add(currentFiling);
				((Map) m.get(FORMAT)).put(t, XbrlUtils.access(info, AccessCmd.Peek, "?", "dimensions", "format"));
				((Set) ((XbrlUtilsFactory) m.get(TAGS)).get(t)).add(currentFiling);
			}
		}
	}

	Pattern ptCellIdx = Pattern.compile("([a-zA-Z]+)(\\d+)");

	private class SheetHandler extends DefaultHandler {
		private SharedStringsTable sst;
		private String lastContents;
		private boolean nextIsString;

		XbrlUtilsFactory taxonomy;

		String col;
		String colTag;
		ArrayList<String> colNameList;
		Map<String, String> colNames = new TreeMap<>();
		Map<String, String> values = new TreeMap<>();
		boolean collectValues;

		private SheetHandler(SharedStringsTable sst) {
			this.sst = sst;
		}

		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			String loc = attributes.getValue("r");
			if ( "row".equals(name) ) {
				collectValues = !("1".equals(loc));
				if ( !collectValues ) {
					colNameList = new ArrayList<>();
					colNames.clear();
					values.clear();
				}
			}

			// c => cell
			if ( name.equals("c") ) {
				Matcher m = ptCellIdx.matcher(loc);
				if ( m.matches() ) {
					col = m.group(1);
				}
				// Figure out if the value is an index in the SST
				String cellType = attributes.getValue("t");
				if ( cellType != null && cellType.equals("s") ) {
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
			if ( nextIsString ) {
				int idx = Integer.parseInt(lastContents);
				lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
				nextIsString = false;
			}

			// v => contents of a cell
			// Output after we've seen the string contents
			if ( name.equals("v") ) {
				if ( collectValues ) {
					values.put(colNames.get(col), lastContents);
				} else {
					if ( colNames.isEmpty() ) {
						colTag = lastContents;
					} else {
						colNameList.add(lastContents);
					}
					colNames.put(col, lastContents);
				}
			} else if ( "row".equals(name) ) {
				if ( collectValues ) {
					String tag = values.get(colTag);
					Map m = (Map) taxonomy.get(tag);
					for (String cn : colNameList) {
						m.put(cn, values.get(cn));
					}
					values.clear();
				} else {
					Map m = (Map) taxonomy.get(META);
					m.put(META, colNameList);
				}
			}
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			lastContents += new String(ch, start, length);
		}
	}

	public void readTaxonomy(String fileName) throws Exception {
		File f = new File(fileName);

		if ( !f.exists() ) {
			return;
		}

		OPCPackage opcp = OPCPackage.open(f);

//		for (PackagePart pp : opcp.getParts()) {
//			String s = pp.getPartName().getName();
//			System.out.println(s);
//		}

		XSSFReader r = new XSSFReader(opcp);
		SharedStringsTable sst = r.getSharedStringsTable();

		SheetHandler handler = new SheetHandler(sst);

		XMLReader reader = XMLReaderFactory.createXMLReader();
		reader.setContentHandler(handler);

		Iterator<InputStream> sheets = r.getSheetsData();

		if ( sheets instanceof XSSFReader.SheetIterator ) {
			XSSFReader.SheetIterator sheetiterator = (XSSFReader.SheetIterator) sheets;

			while (sheetiterator.hasNext()) {
				InputStream sheet = sheetiterator.next();

				String sName = sheetiterator.getSheetName();

				System.out.println("Processing new sheet: " + sName + "\n");

				handler.taxonomy = (XbrlUtilsFactory) namespaces.get(sName).get(TAXONOMY);
				InputSource sheetSource = new InputSource(sheet);
				reader.parse(sheetSource);
				sheet.close();
				System.out.println("");
			}
		}
	}

	public void writeExcel(String fileName) throws Exception {
		Workbook wb;

		Sheet sheet;
		int colCount;

		int rowCount = 0;
		Row row;
		Cell c;
		Hyperlink link;

		wb = fileName.toLowerCase().endsWith(".xlsx") ? new XSSFWorkbook() : new HSSFWorkbook();

	   CreationHelper hlpCreate = wb.getCreationHelper();
	   
		CellStyle rotated = wb.createCellStyle();
		rotated.setRotation((short) 90);

		CellStyle centered = wb.createCellStyle();
		centered.setAlignment(CellStyle.ALIGN_CENTER);
		centered.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

		sheet = wb.createSheet("Filings");

		row = sheet.createRow(rowCount++);

		String[] cols = { "Date added", "Filing ID", "Country", "Period end", "Company", 
				"Err", "Warn", "Inco", "Package URL",  };
		Object[][] paths = { 
				{ "attributes", "date_added" }, 
				{ "attributes", "fxo_id" }, 
				{ "attributes", "country" },
				{ "attributes", "period_end" }, 
				{ "attributes", XbrlFilingManager.ENTITY_NAME }, 
				{ "attributes", "error_count" }, 
				{ "attributes", "warning_count" }, 
				{ "attributes", "inconsistency_count" }, 
				{ "attributes", "package_url" }, 
			};

		for (int i = 0; i < cols.length; ++i) {
			c = row.createCell(i);
			c.setCellValue(cols[i]);
		}

		for (Map.Entry<String, Map> ee : filings.entrySet()) {
			Map key = ee.getValue();
			row = sheet.createRow(rowCount++);

			for (int i = 0; i < cols.length; ++i) {
				c = row.createCell(i);
				String value = XbrlUtils.toString(XbrlUtils.access(key, AccessCmd.Peek, null, paths[i]));
				c.setCellValue(value);
			}
			
			int idx = 1;
			link = hlpCreate.createHyperlink(Hyperlink.LINK_URL);
		  link.setAddress(XbrlFilingManager.XBRL_ORG_ADDR + "/filing/" + XbrlUtils.toString(XbrlUtils.access(key, AccessCmd.Peek, null, paths[idx])));
		  row.getCell(idx).setHyperlink(link);
		  
		  key.put(CELL_ADDRESS, "'Filings'!B" + rowCount);

//		  idx = cols.length - 1;
//			 link = hlpCreate.createHyperlink(Hyperlink.LINK_URL);
//		  String url = XbrlFilingManager.XBRL_ORG_ADDR + XbrlUtils.toString(XbrlUtils.access(key, AccessCmd.Peek, null, paths[idx]));
//			 url = url.replace(" ", "%20");
//			link.setAddress(url);
//		  row.getCell(idx).setHyperlink(link);
		  
		}

		for (int i = 0; i < cols.length; ++i) {
			sheet.autoSizeColumn(i);
		}
		sheet.createFreezePane(2, 1);

		String[] cols2 = { "Name", "Count", "Filings..." };

		sheet = wb.createSheet("Namespaces");
		rowCount = 0;
		row = sheet.createRow(rowCount++);

		for (int i = 0; i < cols2.length; ++i) {
			c = row.createCell(i);
			c.setCellValue(cols2[i]);
		}
		sheet.createFreezePane(2, 1);

		int nsColCount = 0;
		ArrayList<Map> nsContent = new ArrayList<>();

		for (String ns : namespaces.keys()) {
//			System.out.println("Exporting namespace " + ns);
			Map content = namespaces.peek(ns);
			nsContent.add(content);

			Set<String> nsFilings = (Set) content.get(FILINGS);

			row = sheet.createRow(rowCount++);
			colCount = 0;
			c = row.createCell(colCount++);
			c.setCellValue(ns);
			
			String sName = ns + " (" + nsFilings.size() + ")";
		  link = hlpCreate.createHyperlink(Hyperlink.LINK_DOCUMENT);
//		  link.setAddress("'" + sName + "'");
		  link.setAddress("'" + sName + "'!A1");
		  c.setHyperlink(link);
			
			c = row.createCell(colCount++);
			c.setCellValue(nsFilings.size());

			for (String ff : nsFilings) {
				c = row.createCell(colCount++);
				c.setCellValue(ff);
				
				String strLink = (String) filings.get(ff).get(CELL_ADDRESS);
			  link = hlpCreate.createHyperlink(Hyperlink.LINK_DOCUMENT);
			  link.setAddress(strLink);
		  	c.setHyperlink(link);

				if ( colCount > nsColCount ) {
					nsColCount = colCount;
				}
			}
		}

		for (int i = 0; i < nsColCount; ++i) {
			sheet.autoSizeColumn(i);
		}

		nsContent.sort(new Comparator<Map>() {
			@Override
			public int compare(Map o1, Map o2) {
				Set<String> f1 = (Set) o1.get(FILINGS);
				Set<String> f2 = (Set) o2.get(FILINGS);

				int d = f2.size() - f1.size();

				if ( 0 == d ) {
					d = ((String) o1.get(NAME)).compareTo((String) o2.get(NAME));
				}

				return d;
			}
		});

		for (Map content : nsContent) {
			String ns = (String) content.get(NAME);

			Set<String> nsFilings = (Set) content.get(FILINGS);
			List taxCols = Collections.EMPTY_LIST;

			XbrlUtilsFactory taxonomy = (XbrlUtilsFactory) namespaces.get(ns).get(TAXONOMY);
			Map tm = (Map) taxonomy.peek(META);
			if ( null != tm ) {
				taxCols = (List) tm.get(META);
			}

			String sName = ns + " (" + nsFilings.size() + ")";
			Sheet sheetNS = wb.getSheet(sName);

			if ( null == sheetNS ) {
				sheetNS = wb.createSheet(sName);
			} else {
				System.out.println("Repeated sheet: " + sName);
			}
			int rowCountNS = 0;
			row = sheetNS.createRow(rowCountNS++);
			colCount = 0;

			c = row.createCell(colCount++);
			c.setCellValue("Tag");
			c.setCellStyle(centered);

			c = row.createCell(colCount++);
			c.setCellValue("Input format attribute");
			c.setCellStyle(centered);

			for (Object tc : taxCols) {
				c = row.createCell(colCount++);
				c.setCellValue((String) tc);
				c.setCellStyle(centered);
			}

			c = row.createCell(colCount++);
			c.setCellValue("Count");
			c.setCellStyle(rotated);

			for (String ff : nsFilings) {
				c = row.createCell(colCount++);
				c.setCellValue(ff);
				c.setCellStyle(rotated);
				
				String strLink = (String) filings.get(ff).get(CELL_ADDRESS);
			  link = hlpCreate.createHyperlink(Hyperlink.LINK_DOCUMENT);
			  link.setAddress(strLink);
		  	c.setHyperlink(link);
			}
			row.setHeight((short) -1);

			XbrlUtilsFactory<String, Set> tags = (XbrlUtilsFactory) content.get(TAGS);

			for (String tn : tags.keys()) {
				Set<String> tagFilings = (Set) tags.peek(tn);

				row = sheetNS.createRow(rowCountNS++);
				colCount = 0;
				c = row.createCell(colCount++);
				c.setCellValue(tn);

				c = row.createCell(colCount++);
				c.setCellValue((String) XbrlUtils.access(content, AccessCmd.Peek, "?", FORMAT, tn));

				if ( !taxCols.isEmpty() ) {
					tm = (Map) taxonomy.peek(tn);
					if ( null != tm ) {
						for (Object tc : taxCols) {
							c = row.createCell(colCount++);
							c.setCellValue((String) tm.get(tc));
						}
					} else {
						colCount += taxCols.size();
					}
				}

				c = row.createCell(colCount++);
				c.setCellValue(tagFilings.size());

				for (String ff : nsFilings) {
					if ( tagFilings.contains(ff) ) {
						c = row.createCell(colCount);
						c.setCellValue("X");
					}
					++colCount;
				}
			}

			for (int i = 0; i < colCount; ++i) {
				sheetNS.autoSizeColumn(i);
			}
			sheetNS.createFreezePane(taxCols.size() + 3, 1);
		}

		File f = new File(fileName);
		FileOutputStream fileOut = new FileOutputStream(f);

		wb.write(fileOut);
		fileOut.flush();
		fileOut.close();

		wb.close();

	}

	@Override
	public void dump() {
		for (Map.Entry<String, Map> ee : filings.entrySet()) {
			Map key = ee.getValue();
			File f = XbrlUtils.access(key, AccessCmd.Peek, null, "attributes", XbrlFilingManager.REPORT_FILE);
			System.out.println(ee.getKey() + " : " + ((null == f) ? "???" : f.getAbsolutePath()));
		}

		System.out.println(" ==== namespaces ====");

		for (String ns : namespaces.keys()) {
			System.out.println(ns);

			Map content = namespaces.peek(ns);
			System.out.println(" Filings: " + content.get(FILINGS));

			System.out.println(" Tags { ");

			XbrlUtilsFactory<String, Set> tags = (XbrlUtilsFactory) content.get(TAGS);

			for (String tn : tags.keys()) {
				System.out.println("    " + tn + ": " + tags.peek(tn));
			}
			System.out.println(" } \n");
		}
	}

}
