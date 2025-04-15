package com.xbrldock.utils.stream;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.xbrldock.utils.XbrlDockUtils;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class XbrlDockStreamXlsx implements XbrlDockStreamConsts {

	public static Map<String, Map<String, Object>> toMap(File from, Map<String, Object> params) throws Exception {

		Map<String, Map<String, Object>> ret = new TreeMap();
		try (FileInputStream file = new FileInputStream(from); Workbook workbook = new XSSFWorkbook(file)) {

			Collection<Map<String, Object>> members = XbrlDockUtils.simpleGet(params, XDC_GEN_TOKEN_members);

			for (Map<String, Object> cfg : members) {
				Sheet sheet = workbook.getSheet(XbrlDockUtils.simpleGet(cfg, XDC_SPREADHSEET_TOKEN_sheet));

				ArrayList<Map<String, Object>> atts = XbrlDockUtils.simpleGet(cfg, XDC_GEN_TOKEN_attributes);
				Map<String, Map<String, Object>> cols = new TreeMap();

				for (Map<String, Object> a : atts) {
					cols.put((String) a.get(XDC_GEN_TOKEN_source), a);
				}
				
				Map<String, Object> consts = (Map) cfg.getOrDefault(XDC_GEN_TOKEN_members, Collections.EMPTY_MAP);

				ArrayList<String> idCols = XbrlDockUtils.simpleGet(cfg, XDC_EXT_TOKEN_id);

				String[] r = ((String) XbrlDockUtils.simpleGet(cfg, XDC_GEN_TOKEN_rows)).split("-");
				Map<String, Object> rowData = new TreeMap();
				Map<CellRangeAddress, Object> mrv = new HashMap<CellRangeAddress, Object>();

				int start = Integer.valueOf(r[0]) - 1;
				int end = Integer.valueOf(r[1]);

				for (CellRangeAddress cra : sheet.getMergedRegions()) {
					if ((start <= cra.getFirstRow()) && (cra.getLastRow() <= end)) {
						mrv.put(cra, null);
					}
				}

				for (int ri = start; ri < end; ++ri) {
					Row row = sheet.getRow(ri);
					for (Cell cell : row) {
						int ci = cell.getColumnIndex();
						String colName = CellReference.convertNumToColString(ci);
						String colId = XbrlDockUtils.simpleGet(cols, colName, XDC_EXT_TOKEN_id);
						if (null == colId) {
							continue;
						}
						int cellType = cell.getCellType();

						Object v = null;
						switch (cellType) {
						case Cell.CELL_TYPE_STRING:
							v = cell.getStringCellValue();
							break;
						case Cell.CELL_TYPE_NUMERIC:
							v = cell.getNumericCellValue();
							break;
						case Cell.CELL_TYPE_BOOLEAN:
							v = cell.getBooleanCellValue();
							break;
						case Cell.CELL_TYPE_FORMULA:
							v = cell.getNumericCellValue();
//							v = cell.getCellFormula();
							break;
						default:
							break;
						}

						if (null == v) {
							for (CellRangeAddress cra : mrv.keySet()) {
								if (cra.isInRange(ri, ci)) {
									v = mrv.get(cra);
									if (null != v) {
										rowData.put(colId, v);
									}
									break;
								}
							}
						} else {
							rowData.put(colId, v);

							for (CellRangeAddress cra : mrv.keySet()) {
								if (cra.isInRange(ri, ci)) {
									mrv.put(cra, v);
								}
							}
						}
					}

					if (!rowData.isEmpty()) {
						rowData.putAll(consts);
						StringBuilder sb = null;

						for (String ik : idCols) {
							sb = XbrlDockUtils.sbAppend(sb, "_", true, XbrlDockUtils.toKey(rowData.get(ik)));
						}

						ret.put(sb.toString(), rowData);

						rowData = new TreeMap();
					}

				}
			}
		}

		return ret;
	}

}
