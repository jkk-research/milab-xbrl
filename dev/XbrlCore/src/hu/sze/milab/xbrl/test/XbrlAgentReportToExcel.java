package hu.sze.milab.xbrl.test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts;
import hu.sze.milab.dust.DustMetaConsts;
import hu.sze.milab.dust.stream.DustStreamConsts;

@SuppressWarnings("rawtypes")
public class XbrlAgentReportToExcel implements DustMetaConsts, DustStreamConsts, DustConsts.MindAgent {
	private static final List<String> FACT_ELEMENTS = Arrays.asList("ix:nonFraction", "ix:nonNumeric");

	private static final String VAL_COL = "VALUE";

	private static final List[] FACT_ATTS = new List[] { Arrays.asList("name", "id", "contextRef", "unitRef", VAL_COL, "scale", "decimals", "format"),
			Arrays.asList("name", "id", "contextRef", "xml:lang", "continuedAt", VAL_COL) };

	private MindHandle hTarget;
	String fileName;

	private Workbook wb;
	private List<Sheet> sheets = null;

	private Row row;
	private List readAtts = null;

	public void init(MindHandle hTarget, String fileName) {
		this.hTarget = hTarget;
		this.fileName = fileName;
		wb = fileName.toLowerCase().endsWith(".xlsx") ? new XSSFWorkbook() : new HSSFWorkbook();
		sheets = null;
		readAtts = null;
		row = null;
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
		Object name = Dust.access(hTarget, MindAccess.Peek, null, TEXT_ATT_NAMED_NAME);

		switch ( action ) {
		case Begin:
			idx = FACT_ELEMENTS.indexOf(name);

			if ( -1 != idx ) {
				Sheet sheet;

				if ( null == sheets ) {
					sheets = new ArrayList<>();
					for (int si = 0; si < FACT_ELEMENTS.size(); ++si) {
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
			}
			break;
		case Process:
			if ( null != row ) {
				Object itemType = Dust.access(hTarget, MindAccess.Peek, null, MIND_ATT_KNOWLEDGE_TAG);
				idx = -1;

				if ( itemType == XmlData.Attribute ) {
					idx = readAtts.indexOf(name);
				} else if ( itemType == XmlData.Content ) {
					idx = readAtts.indexOf(VAL_COL);
				}

				if ( -1 != idx ) {
					String strVal = Dust.access(hTarget, MindAccess.Peek, null, MISC_ATT_VARIANT_VALUE);

					Cell c = row.createCell(idx);
					c.setCellValue(strVal);
				}
			}
			break;
		case End:
			row = null;
			readAtts = null;
			break;
		default:
			break;
		}

		return MindStatus.ReadAccept;
	}
}
