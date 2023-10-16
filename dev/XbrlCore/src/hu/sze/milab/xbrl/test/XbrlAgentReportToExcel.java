package hu.sze.milab.xbrl.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts;
import hu.sze.milab.xbrl.XbrlConsts;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlAgentReportToExcel implements XbrlConsts, DustConsts.MindAgent {
	private static final List<String> FACT_ELEMENTS = Arrays.asList("ix:nonFraction", "ix:nonNumeric");

	private static final String VAL_COL = "VALUE";
	private static final String VAL_COLNUM = "NumValue";

	private static final List[] FACT_ATTS = new List[FACT_ELEMENTS.size()];

	private static final List<String> CONTEXT_COLS = new ArrayList<>();
	private static final String DIM_COL = "xbrldi:explicitMember";
	private static final String CONTEXT_ELEMENT = "xbrli:context";

	private MindHandle hTarget;
	String fileName;

	private Workbook wb;
	private List<Sheet> sheets = null;

	private XbrlInfoType readInfoType;
	private String readCtxId;
	private String readKey;

	private Row row;
	private List readAtts = null;
	private Map<String, String> numData = new HashMap<>();

	public void init(MindHandle hTarget, String fileName) {
		this.hTarget = hTarget;
		this.fileName = fileName;
		wb = fileName.toLowerCase().endsWith(".xlsx") ? new XSSFWorkbook() : new HSSFWorkbook();
		sheets = null;
		readAtts = null;
		row = null;
		CONTEXT_COLS.clear();
		CONTEXT_COLS.addAll(Arrays.asList("xbrli:identifier", "xbrli:instant", "xbrli:startDate", "xbrli:endDate"));

		FACT_ATTS[0] = new ArrayList(Arrays.asList("name", "id", "contextRef", "unitRef", VAL_COL, "scale", "decimals", "format", VAL_COLNUM));
		FACT_ATTS[1] = new ArrayList(Arrays.asList("name", "id", "contextRef", "xml:lang", "continuedAt", VAL_COL));

		Dust.access(hTarget, MindAccess.Reset, null, "Contexts");
	}

	public File save() throws Exception {
		File f = null;

		if ( null != sheets ) {
			f = new File(fileName);
			FileOutputStream fileOut = new FileOutputStream(f);

			wb.write(fileOut);
			fileOut.flush();
			fileOut.close();

			wb.close();
		}

		return f;
	}

	@Override
	public MindStatus agentExecAction(MindAction action) throws Exception {
		int idx;
		String name = Dust.access(hTarget, MindAccess.Peek, null, TEXT_ATT_NAME);

		switch ( action ) {
		case Begin:
			idx = FACT_ELEMENTS.indexOf(name);

			if ( -1 != idx ) {
				readInfoType = XbrlInfoType.Fact;

				Sheet sheet;

				if ( null == sheets ) {
					sheets = new ArrayList<>();
					for (int si = 0; si < FACT_ELEMENTS.size(); ++si) {
						for (int ci = CONTEXT_COLS.size(); ci-- > 0;) {
							FACT_ATTS[si].add(3, CONTEXT_COLS.get(ci).split(":")[1]);
						}
//						FACT_ATTS[si].addAll(3, CONTEXT_COLS);

						String sn = FACT_ELEMENTS.get(si).split(":")[1];
						sheet = wb.createSheet(sn);
						sheets.add(sheet);

						row = sheet.createRow(0);
						readAtts = FACT_ATTS[si];
						for (int ai = 0; ai < readAtts.size(); ++ai) {
							Cell c = row.createCell(ai);
							c.setCellValue((String) readAtts.get(ai));
						}
					}
				}

				readAtts = FACT_ATTS[idx];
				sheet = sheets.get(idx);
				int rowNum = sheet.getLastRowNum();
				row = sheet.createRow(rowNum + 1);

				numData.put(VAL_COL, null);
				numData.put("scale", null);
				numData.put("decimals", null);
				numData.put("format", null);
			} else if ( name.equals(CONTEXT_ELEMENT) ) {
				readInfoType = XbrlInfoType.Context;
				readKey = name;
			} else if ( readInfoType == XbrlInfoType.Context ) {
				readKey = name;
			}
			break;
		case Process:
			Object itemType = Dust.access(hTarget, MindAccess.Peek, null, MIND_ATT_KNOWLEDGE_TAGS);
			String strVal = Dust.access(hTarget, MindAccess.Peek, null, MISC_ATT_VARIANT_VALUE);

			if ( null != row ) {
				idx = -1;

				if ( itemType == XmlData.Attribute ) {
					if ( "contextRef".equals(name) ) {
						Map mm = Dust.access(hTarget, MindAccess.Peek, null, "Contexts", strVal);
						if ( null == mm ) {
							Dust.dumpObs("Context ref not resolved", strVal);
						} else {
							for (Object e : mm.entrySet()) {
								idx = readAtts.indexOf(((Map.Entry) e).getKey());
								if ( -1 != idx ) {
									Cell c = row.createCell(idx);
									c.setCellValue((String) ((Map.Entry) e).getValue());
								}
							}
						}
					}

					idx = readAtts.indexOf(name);
				} else if ( itemType == XmlData.Content ) {
					idx = readAtts.indexOf(VAL_COL);
					name = VAL_COL;
				}

				if ( -1 != idx ) {
					Cell c = row.createCell(idx);
					c.setCellValue(strVal);

					if ( numData.containsKey(name) ) {
						numData.put(name, strVal);
					}
				}

			} else if ( readInfoType == XbrlInfoType.Context ) {
				if ( itemType == XmlData.Attribute ) {
					if ( readKey.equals(CONTEXT_ELEMENT) && "id".equals(name) ) {
						readCtxId = strVal;
					} else if ( readKey.equals(DIM_COL) && "dimension".equals(name) ) {
						readKey = strVal;
						if ( !CONTEXT_COLS.contains(readKey) ) {
							CONTEXT_COLS.add(readKey);
						}
					}
				} else if ( itemType == XmlData.Content ) {
					if ( CONTEXT_COLS.contains(readKey) ) {
						Dust.access(hTarget, MindAccess.Set, strVal, "Contexts", readCtxId, readKey.split(":")[1]);
					}
					readKey = null;
				}
			}
			break;
		case End:
			if ( name.equals(CONTEXT_ELEMENT) ) {
				readInfoType = null;
				readKey = null;
			}

			if ( (null != row) && (readAtts == FACT_ATTS[0]) ) {
				Double val = 0.0;
				String strErr = null;

				if ( "ixt4:num-comma-decimal".equals(numData.getOrDefault("format", "ixt4:fixed-zero")) ) {
					strVal = numData.get(VAL_COL);
					if ( null != strVal ) {
						try {
							val = Double.parseDouble(strVal);
						} catch (Throwable e) {
							strErr = e.toString();
						}
						strVal = numData.get("scale");
						if ( null != strVal ) {
							int scale = Integer.parseInt(strVal);
							if ( 0 < scale ) {
								double sc = Math.pow(10, scale);
								val = val * sc;
							}
						}
					}
				}

				idx = readAtts.indexOf(VAL_COLNUM);

				Cell c = row.createCell(idx);
				if ( null == strErr ) {
					c.setCellValue(val);
				} else {
					c.setCellValue(strErr);
				}
			}
			row = null;
			readAtts = null;
			break;
		default:
			break;
		}

		return MindStatus.ReadAccept;
	}
}
