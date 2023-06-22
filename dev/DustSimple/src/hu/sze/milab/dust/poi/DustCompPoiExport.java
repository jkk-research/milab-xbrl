package com.gollywolly.dustcomp.dust.io.poi;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.gollywolly.dustcomp.DustCompServices.DustUtilsExplorer;
import com.gollywolly.dustcomp.DustCompServices.DustUtilsStateful;
import com.gollywolly.dustcomp.DustCompUtils;
import com.gollywolly.dustcomp.api.DustCompApiHacks;
import com.gollywolly.dustcomp.api.DustCompApiUtils;
import com.gollywolly.dustcomp.coll.DustCompCollections;
import com.gollywolly.dustcomp.coll.DustCompUtilsLazyCreator;
import com.gollywolly.dustcomp.shared.DustCompSerializeNames;

public class DustCompPoiExport implements DustUtilsStateful, DustUtilsExplorer, DustCompPoiComponents, DustCompCollections, DustCompSerializeNames {

	int dEntity, dType, dValue;
	String fileName;
	Workbook wb;

	Sheet sheet;
	Row rowType, rowCol;
	int colCount;

	int rowCount;
	Row rowContent;
	int depth;
	TypeInfo currType;
	String fldName;

	class TypeInfo {
		String typeName;
		Map<String, Integer> columns = new HashMap<>();

		public TypeInfo(String key) {
			DustIdentifier idType = idFact.smartId(key);
			typeName = (String) DustCompApiUtils.getValue(DustIdentifier.ICTX_GLOBAL, new DustIdentifier[] { TYPE_TYPE, idType, TYPE_TEXT_HUMANINFO, FLD_LABEL });

			DustCompApiUtils.visit(idFact.accessPath(DustIdentifier.ICTX_GLOBAL, new DustIdentifier[] { TYPE_TYPE, idType, FLD_FIELDS }), new DustValueVisitorDefault() {
				@Override
				protected void process(Object value, int idx) throws Exception {
					String label = (String) DustCompApiUtils.getValue(VISIT_LABEL);
					int cIdx = colCount + columns.size();
					columns.put(DustCompUtils.toId(label), cIdx);

					Cell c = rowCol.createCell(cIdx);
					c.setCellValue(label);
				}

				@Override
				public void endField(int response, DustVisitChangeCallback ccb) throws Exception {
					int colEnd = colCount + columns.size();

					Cell c = rowType.createCell(colCount);
					sheet.addMergedRegion(new CellRangeAddress(0, 0, colCount, colEnd - 1));
					c.setCellValue(typeName);

					colCount = colEnd;
				}
			});

		}
	}

	DustCompUtilsLazyCreator types = new DustCompUtilsLazyCreator(new DustFactory() {
		public Object create(Object key) {
			return new TypeInfo((String) key);
		}
	});

	private void initSheet() throws Exception {
		String sheetName = (String) DustCompApiUtils.getValue(FLD_NAME);
		sheet = wb.createSheet(sheetName);

		rowType = sheet.createRow(0);
		rowCol = sheet.createRow(1);
		rowContent = null;

		types.clear();
		colCount = 0;
		rowCount = 2;
	}

	@Override
	public void dust_utils_stateful_init() throws Exception {
		fileName = (String) DustCompApiUtils.getParamValue(VAR_TEXT);
		wb = fileName.toLowerCase().endsWith(".xlsx") ? new XSSFWorkbook() : new HSSFWorkbook();

		dType = DustCompApiHacks.getInt(DustIdentifier.ICTX_THIS, FLD_EXP_TYPEDEPTH, 1);
		dValue = dType + 1;
		dEntity = dType - 1;
	}

	@Override
	public void dust_utils_stateful_release() throws Exception {
		File f = new File(fileName);
		f.getParentFile().mkdirs();
		FileOutputStream fileOut = new FileOutputStream(f);

		wb.write(fileOut);
		fileOut.flush();
		fileOut.close();
	}

	@Override
	public void dust_utils_block_start() throws Exception {
		++depth;

		String bm = (String) DustCompApiUtils.getParamValue(FLD_BLOCKTYPE);

		if (SERIALIZE_BLOCKTYPE_MASTER.equals(bm)) {
			initSheet();
		}
	}

	@Override
	public void dust_utils_visitor_startField() throws Exception {
		fldName = (String) DustCompApiUtils.getParamValue(FLD_ID);
	}

	@Override
	public void dust_utils_visitor_processValue() throws Exception {
		Object value = DustCompApiUtils.getParamValue(VAR_TEXT);

		if (depth == dValue) {
			if (null != value) {
				if (null == rowContent) {
					rowContent = sheet.createRow(rowCount++);
				}
				Integer idx = currType.columns.get(fldName);
				if (null != idx) {
					Cell c = rowContent.createCell(idx);
					c.setCellValue(value.toString());
				}
			}
		} else if (depth == dType) {
			if (FLD_TYPEID.toString().equals(fldName)) {
				currType = (TypeInfo) types.get(value);
			}
		}
	}

	@Override
	public void dust_utils_visitor_endField() throws Exception {
	}

	@Override
	public void dust_utils_block_end() throws Exception {
		--depth;
		if (depth == dEntity) {
			rowContent = null;
		} else {
			String bm = (String) DustCompApiUtils.getParamValue(FLD_BLOCKTYPE);

			if (SERIALIZE_BLOCKTYPE_MASTER.equals(bm)) {
				if (null != sheet) {
					for (int i = 0; i < colCount; ++i) {
						sheet.autoSizeColumn(i);
					}
				}
			}

		}
	}
}
