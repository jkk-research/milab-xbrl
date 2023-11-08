package hu.sze.milab.xbrl.test;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.dev.DustDevCounter;
import hu.sze.milab.dust.stream.DustStreamUtils;
import hu.sze.milab.dust.stream.xml.DustStreamXmlConsts;
import hu.sze.milab.dust.stream.xml.DustStreamXmlDocumentGraphLoader;
import hu.sze.milab.dust.utils.DustUtils;
import hu.sze.milab.dust.utils.DustUtils.QueueContainer;
import hu.sze.milab.dust.utils.DustUtilsData;
import hu.sze.milab.dust.utils.DustUtilsFactory;
import hu.sze.milab.dust.utils.DustUtilsFile;

class XbrlTaxonomyLoaderExcel implements DustStreamXmlDocumentGraphLoader.XmlDocumentProcessor, DustStreamXmlConsts {

	interface CellUpdater {
		String update(String oldVal, String val);
	}

	CellUpdater concat = new CellUpdater() {

		@Override
		public String update(String oldVal, String val) {
			return oldVal.contains(val) ? oldVal : oldVal + "\n" + val;
		}
	};

	class NamespaceData {
		Element e;
		Map<String, Element> items = new TreeMap<>();

		public NamespaceData(Element eNS) {
			this.e = eNS;
		}
	}

	class LinkData {
		Element link;
		Element from;
		Element to;

		public LinkData(Element link, Map<String, Element> links) {
			this.link = link;

			String ref = link.getAttribute("xlink:from");
			this.from = links.get(ref);
			ref = link.getAttribute("xlink:to");
			this.to = links.get(ref);
		}
	}

	DustDevCounter linkInfo = new DustDevCounter(true);
	Set<String> allFiles = new TreeSet<>();

	Map<String, NamespaceData> namespaces = new TreeMap<>();
	Map<String, NamespaceData> nsByUrl = new TreeMap<>();

	Set<LinkData> allLinks = new HashSet<>();
	@SuppressWarnings({ "unchecked", "rawtypes" })
	DustUtilsFactory<String, Set<LinkData>> locLinks = new DustUtilsFactory.Simple(true, HashSet.class);

	File root;
	Map<String, String> uriRewrite;
	
	public XbrlTaxonomyLoaderExcel(File root, Map<String, String> uriRewrite) {
		this.root = root;
		this.uriRewrite = uriRewrite;

		allFiles.clear();
		addFolder(root);

		Dust.dumpObs("Files to visit in folder", root.getName(), allFiles.size());		
	}

	void addFolder(File dir) {
		for (File f : dir.listFiles()) {
			if ( f.isFile() ) {
				if ( f.getName().startsWith(".") ) {
					continue;
				}
				try {
					allFiles.add(f.getCanonicalPath());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else if ( f.isDirectory() ) {
				addFolder(f);
			}
		}
	}

	public void setSeen(File... files) throws Exception {
		for (File f : files) {
			allFiles.remove(f.getCanonicalPath());
		}
	}

	public void dump() throws Exception {
		if ( allFiles.isEmpty() ) {
			Dust.dumpObs("All files visited");
		} else {
			Dust.dumpObs("Unseen files");

			for (String s : allFiles) {
				Dust.dumpObs("   ", s);
			}
		}

		Set<String> skipUrl = new TreeSet<>();

		for (String conceptRef : locLinks.keys()) {
			String[] ref = conceptRef.split("#");
			
			NamespaceData nsd = nsByUrl.get(ref[0]);
			if ( null == nsd ) {
				skipUrl.add(ref[0]);
			} else {
				Element e = nsd.items.get(ref[1]);
				if ( null == e ) {
					Dust.dumpObs("Referred concept not found", conceptRef);
				}
			}
		}

		for (String uu : skipUrl) {
			Dust.dumpObs("Referred url not found", uu);
		}

		linkInfo.dump("Links");
	}

	DustUtilsData.Indexer<String> attCols = new DustUtilsData.Indexer<String>();
	private static final String[] KNOWN_ATT_COLS = { "name", "id", "type", "substitutionGroup", "xbrli:periodType", "Parent", "Children", "http://www.xbrl.org/2003/role/label",
//			"abstract", "nillable", "xbrli:balance", "cyclesAllowed", "roleURI", "arcroleURI",
//			"http://www.xbrl.org/2003/role/label", "http://www.xbrl.org/2003/role/documentation" 
	};

	private Workbook wb;
	CellStyle csLabel;
	CellStyle csHeader;

	private int colCount;

	public void save(File dir, String taxName) throws Exception {
		for (String a : KNOWN_ATT_COLS) {
			attCols.getIndex(a);
		}
		colCount = attCols.getSize();

		wb = new XSSFWorkbook();

		CreationHelper hlpCreate = wb.getCreationHelper();

		csLabel = wb.createCellStyle();
		csLabel.setWrapText(true);

		csHeader = wb.createCellStyle();
		csHeader.setAlignment(CellStyle.ALIGN_CENTER);
		csHeader.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

		Sheet sheet;
		Row row;
		ArrayList<String> sheetNames = new ArrayList<>();

		for (Map.Entry<String, NamespaceData> ne : namespaces.entrySet()) {
			String nsOrig = ne.getKey();
			String ns = DustStreamUtils.cutExcelSheetName(nsOrig);

			Dust.dumpObs("  Creating sheet", ns, "for namespace", nsOrig);
			sheet = wb.createSheet(ns);
			sheetNames.add(ns);

			int rc = 0;

			row = sheet.createRow(rc++);
			for (String an : attCols.keys()) {
				Cell c;
				int ax = attCols.getIndex(an);

				if ( an.startsWith("http:") ) {
					int sep = an.lastIndexOf("/");
					if ( -1 != sep ) {
						an = an.substring(sep + 1);
					}

					if ( !"deprecatedDateLabel".equals(an) ) {
						sheet.setColumnWidth(ax, 80 * 256);
					}
				}

				c = row.createCell(ax);
				c.setCellValue(an);
				c.setCellStyle(csHeader);
			}

			NamespaceData nd = ne.getValue();
			String url = (String) nd.e.getUserData(XML_DATA_DOCURL);

			for (Map.Entry<String, Element> ie : nd.items.entrySet()) {
				Element e = ie.getValue();
				row = sheet.createRow(rc++);

				NamedNodeMap atts = e.getAttributes();
				int ac = atts.getLength();
				for (int ai = 0; ai < ac; ++ai) {
					Node a = atts.item(ai);
					storeValue(row, a.getNodeName(), a.getNodeValue(), concat);
				}

				String eid = ie.getKey();
				String id = url + "#" + eid;

				Set<LinkData> lds = locLinks.peek(id);

				if ( null != lds ) {
					for (LinkData ld : lds) {

						String arcType = ld.link.getTagName();
						String role = null;
						String val = null;
						CellStyle cs = csLabel;
						String uri = null;
						String co = null;

						NodeList enl;
						int sep;
						CellUpdater u = concat;

						switch ( arcType ) {
						case "X link:labelArc":
							role = ld.to.getAttribute("xlink:role");
							val = ld.to.getTextContent();
							break;
						case "link:referenceArc":
							role = ld.to.getAttribute("xlink:role");

							enl = ld.to.getChildNodes();
							StringBuilder sb = new StringBuilder();

							for (int i = 0; i < enl.getLength(); ++i) {
								Node item = enl.item(i);
								if ( item instanceof Element ) {
									Element ce = (Element) item;
									String ceVal = ce.getTextContent();
									if ( !DustUtils.isEmpty(ceVal) ) {
										String cetn = ce.getTagName();
										if ( cetn.endsWith(":URI") ) {
											uri = ceVal;
										} else {
											sb.append(cetn).append(": ").append(ceVal).append(", ");
										}
									}
								}
							}
							val = sb.toString();
							break;
						case "link:presentationArc":
							if ( ld.to.getAttribute("xlink:href").endsWith(eid) ) {
								role = "Parent";
								val = ld.from.getAttribute("xlink:href");
							} else if ( ld.from.getAttribute("xlink:href").endsWith(eid) ) {
								co = ld.link.getAttribute("order");
								role = "Children";
								val = ld.to.getAttribute("xlink:href");
							} else {
								Dust.dumpObs("huh?");
							}

							break;
						case "link:definitionArc":
							role = ld.link.getAttribute("xlink:arcrole");
							if ( ld.to.getAttribute("xlink:href").endsWith(eid) ) {
								role = role + " parent";
								val = ld.from.getAttribute("xlink:href");
							} else if ( ld.from.getAttribute("xlink:href").endsWith(eid) ) {
								role = role + " child";
								val = ld.to.getAttribute("xlink:href");
							} else {
								Dust.dumpObs("huh?");
							}

							break;
						default:
							continue;
						}

						if ( null != role ) {
							sep = val.indexOf("#");
							if ( -1 != sep ) {
								val = val.substring(sep + 1);
							}

							if ( null != co ) {
								val = co + " " + val;
							}
							Cell c = storeValue(row, role, val, u);
							if ( null != cs ) {
								c.setCellStyle(csLabel);
							}

							if ( null != uri ) {
								Hyperlink link = hlpCreate.createHyperlink(Hyperlink.LINK_URL);
								uri = uri.replace(" ", "%20");
								link.setAddress(uri);
								c.setHyperlink(link);
							}
						}
					}
				}
			}

			sheet.createFreezePane(1, 1);
		}

		sheetNames.sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return wb.getSheet(o2).getLastRowNum() - wb.getSheet(o1).getLastRowNum();
			}
		});

		for (int i = 0; i < sheetNames.size(); ++i) {
			wb.setSheetOrder(sheetNames.get(i), i);
		}

		File f = new File(dir, taxName + ".xlsx");
		FileOutputStream fileOut = new FileOutputStream(f);

		wb.write(fileOut);
		fileOut.flush();
		fileOut.close();

		wb.close();
	}

	public Cell storeValue(Row row, String colName, String value, CellUpdater updater) {
		Cell c;
		int ax = attCols.getIndex(colName);

		if ( ax >= colCount ) {
			addAttCol(colName);
			colCount = ax + 1;
		}

		c = row.getCell(ax);

		if ( null == c ) {
			c = row.createCell(ax);
		} else {
			value = updater.update(c.getStringCellValue(), value);
		}
		if ( value.length() > 32767 ) {
			value = value.substring(0, 32700) + " ... [truncated!]";
		}
		c.setCellValue(value);

		return c;
	}

	private void addAttCol(String an) {
		int cw = 0;

//		if ( an.startsWith("http:") ) 
		{
			int sep = an.lastIndexOf("/");
			if ( -1 != sep ) {
				an = an.substring(sep + 1);
				if ( !"deprecatedDateLabel".equals(an) ) {
					cw = 80 * 256;
				}
			}

		}

		for (Iterator<Sheet> it = wb.sheetIterator(); it.hasNext();) {
			Sheet sheet = it.next();
			Cell c = sheet.getRow(0).createCell(colCount);
			c.setCellValue(an);
			c.setCellStyle(csHeader);

			if ( 0 < cw ) {
				sheet.setColumnWidth(colCount, cw);
			}
		}
	}

	@Override
	public void documentLoaded(Element root, QueueContainer<String> loader) {
		NodeList el;

//		String url = root.getAttribute(XML_ATT_REF);
		String url = (String) root.getUserData(XML_DATA_DOCURL);

		if ( url.startsWith("file") ) {
			try {
				File f = Paths.get(new URL(url).toURI()).toFile();
				setSeen(f);
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

//		Dust.dumpObs("  Reading url", url);

//		if ( root.getTagName().endsWith(":schema") ) 
		{
			NamespaceData nsd = null;

			el = root.getElementsByTagName("*");
			for (int ei = el.getLength(); ei-- > 0;) {
				Element e = (Element) el.item(ei);

				String id = e.getAttribute("id");
				if ( !DustUtils.isEmpty(id) ) {
					if ( null == nsd ) {
						nsd = new NamespaceData(root);
						String ns = root.getAttribute("targetNamespace");
						if ( !DustUtils.isEmpty(ns) ) {
							namespaces.put(ns, nsd);
							Dust.dumpObs("      Registered namespace", ns);
						}
						nsByUrl.put(url, nsd);
					}

					nsd.items.put(id, e);
				}
			}
		}

		if ( root.getTagName().endsWith("linkbase") )

		{
//			Dust.dumpObs("      Linkbase found", url);

			Map<String, Element> labeledLinks = new TreeMap<>();

			String[] LINK_ATTS = { "xlink:type", "xlink:role", "xlink:arcrole" };

			el = root.getElementsByTagName("*");
			for (int ei = el.getLength(); ei-- > 0;) {
				Element e = (Element) el.item(ei);

				String lbl = e.getAttribute("xlink:label");
				if ( !DustUtils.isEmpty(lbl) ) {
					labeledLinks.put(lbl, e);
				}

				String tn = e.getTagName();
				if ( tn.startsWith("link") ) {
					linkInfo.add(tn);

					for (String an : LINK_ATTS) {
						String av = e.getAttribute(an);
						if ( !DustUtils.isEmpty(av) ) {
							linkInfo.add(tn + " " + an + ": " + av);
						}
					}
				}
			}

			el = root.getElementsByTagName("*");
			for (int ei = el.getLength(); ei-- > 0;) {
				Element e = (Element) el.item(ei);

				String type = e.getAttribute("xlink:type");

				if ( "arc".equals(type) ) {
					LinkData ld = new LinkData(e, labeledLinks);

					allLinks.add(ld);

					String ref = ld.from.getAttribute("xlink:href");
					if ( !DustUtils.isEmpty(ref) ) {
						ref = optLocalizeUrl(ref, url);
						locLinks.get(ref).add(ld);
					}
					ref = ld.to.getAttribute("xlink:href");
					if ( !DustUtils.isEmpty(ref) ) {
						ref = optLocalizeUrl(ref, url);
						locLinks.get(ref).add(ld);
					}
				}
			}
		}

		el = root.getElementsByTagName("link:linkbaseRef");
		for (int ei = el.getLength(); ei-- > 0;) {
			String href = ((Element) el.item(ei)).getAttribute("xlink:href");
			String refUrl = optLocalizeUrl(href, url);
			loader.enqueue(href, refUrl);
		}
	}

	public String optLocalizeUrl(String href, String url) {
		String refUrl = href;

		if ( !href.contains(":") ) {
			refUrl = DustUtils.replacePostfix(url, "/", href);
		} else {
			for (Map.Entry<String, String> e : uriRewrite.entrySet()) {
				String prefix = e.getKey();
				if ( url.startsWith(prefix) ) {
					File f = new File(root, e.getValue());
					f = new File(f, url.substring(prefix.length()));
					try {
						refUrl = f.toURI().toURL().toString();
					} catch (Throwable e1) {
						e1.printStackTrace();
					}
					break;
				}
			}
		}

		refUrl = DustUtilsFile.optRemoveUpFromPath(refUrl);

		return refUrl;
	}
}