package hu.sze.milab.xbrl.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.stream.xml.DustStreamXmlLoader;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtils.QueueContainer;

class XbrlTaxonomyToExcel implements DustStreamXmlLoader.NamespaceProcessor {
		DustUtils.Indexer<String> attCols = new DustUtils.Indexer<String>();
		private static final String[] KNOWN_ATT_COLS = {"name", "id", "type", "substitutionGroup", "xbrli:periodType", "abstract"};

		private Workbook wb;

		private int colCount;
		
		public XbrlTaxonomyToExcel() {
			for ( String a : KNOWN_ATT_COLS ) {
				attCols.getIndex(a);
			}
		}

		@Override
		public void namespaceLoaded(Element root, QueueContainer<String> loader) {
			if ( null == wb ) {
				wb = new XSSFWorkbook();
				colCount = attCols.getSize();
			}

			int rc = 0;

			Sheet sheet = null;
			Row row;
			Cell c;

			NodeList el = root.getElementsByTagName("xsd:element");
			int ec = el.getLength();
			for (int ei = 0; ei < ec; ++ei) {
				NamedNodeMap atts = el.item(ei).getAttributes();

				if ( null != atts ) {
					if ( null == sheet ) {
						String nsOrig = root.getAttribute("targetNamespace");
						String ns = nsOrig;
						int sep = ns.indexOf("://");
						if ( -1 != sep ) {
							ns = ns.substring(sep + 3);
						}
						ns = ns.replace("/", "_");
						
						int nl = ns.length();
						if ( 31 <= nl ) {
							ns = ns.substring(nl - 31, nl);
//							sep = ns.indexOf("_");
//							String s1 = ns.substring(0, sep);
//							String s2 = ns.substring(nl - 31 + sep + 2, nl);
//							ns = s1 + ".." + s2;
//							nl = ns.length();
						}
						
						sheet = wb.getSheet(ns);
						
						if ( null != sheet ) {
							Dust.dumpObs("  Skipping duplicated sheet", ns, "for namespace", nsOrig);
							return;
						}

						Dust.dumpObs("  Creating sheet", ns, "for namespace", nsOrig);
						sheet = wb.createSheet(ns);

						row = sheet.createRow(rc++);
						for (String k : attCols.keys()) {
							c = row.createCell(attCols.getIndex(k));
							c.setCellValue(k);
						}
					}

					row = sheet.createRow(rc++);
					int ac = atts.getLength();
					for (int ai = 0; ai < ac; ++ai) {
						Node a = atts.item(ai);
						String an = a.getNodeName();

						int ax = attCols.getIndex(an);

						if ( ax >= colCount ) {
							addAttCol(an);
							colCount = ax + 1;
						}

						c = row.createCell(ax);
						c.setCellValue(a.getNodeValue());
					}
				}
			}
		}

		private void addAttCol(String an) {
			for (Iterator<Sheet> it = wb.sheetIterator(); it.hasNext();) {
				Cell c = it.next().getRow(0).createCell(colCount);
				c.setCellValue(an);
			}
		}

		public void save(File dir, String taxName) throws Exception {
			File f = null;

			if ( null != wb ) {
				f = new File(dir, taxName + ".xlsx");
				FileOutputStream fileOut = new FileOutputStream(f);

				wb.write(fileOut);
				fileOut.flush();
				fileOut.close();

				wb.close();

				wb = null;
			}
		}
	}