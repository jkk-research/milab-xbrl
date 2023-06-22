package com.gollywolly.dustcomp.dust.io.poi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.gollywolly.dustcomp.DustCompServices.DustUtilsProcessor;
import com.gollywolly.dustcomp.DustCompUtils;
import com.gollywolly.dustcomp.api.DustCompApiHacks;
import com.gollywolly.dustcomp.api.DustCompApiUtils;
import com.gollywolly.dustcomp.coll.DustCompCollections;
import com.gollywolly.dustcomp.dust.geocoding.DustGeocoderNames;
import com.gollywolly.dustcomp.shared.DustCompGeocoderNames;
import com.gollywolly.dustcomp.utils.DustCompUtilsDateFormatter;

public class DustCompPoiImport implements DustUtilsProcessor, DustCompPoiComponents, DustCompCollections {
	DustIdentifierFactory idFact = DustCompApiUtils.getPathBuilder();

	@Override
	public void dust_utils_processor_process() throws Exception {
		DustIdentifier idPath = idFact.accessPath(DustIdentifier.ICTX_PARAM, new DustIdentifier[] { TYPE_VARIANT, VT_LINK, FLD_SOURCE, FLD_PATH });
		String path = (String) DustCompApiUtils.getParamValue(idPath);

		evaluate(path);
	}

	DustIdentifier initSheet(String importId, String sheetName, DustIdentifier[] idArr) {
		String sheetLabel;
		DustIdentifier idSheet;

		if (sheetName.startsWith("!")) {
			sheetName = sheetName.substring(1);
			int sep = sheetName.indexOf("!");
			if (1 > sep) {
				return null;
			} else {
				sheetLabel = sheetName.substring(sep + 1);
				sheetName = sheetName.substring(0, sep);
			}
		} else {
			sheetLabel = sheetName;
		}

		idSheet = idFact.smartId("ERPort:Data:" + importId + "_" + DustCompUtils.toId(sheetName));
		Object newType = DustCompApiUtils.getGlobalValue(TYPE_TYPE, idSheet);

		DustCompApiUtils.changeValue(idFact.accessPath(DustIdentifier.ICTX_PARAM, new DustIdentifier[] { TYPE_VARIANT, VT_LINK, TYPE_CONTAINER, FLD_CHILDREN }),
				DustChangeCommand.INSERT, newType, -1);

		idArr[1] = idSheet;
		idArr[2] = TYPE_TEXT_HUMANINFO;
		idArr[3] = FLD_LABEL;

		DustCompApiUtils.setValue(idFact.accessPath(DustIdentifier.ICTX_GLOBAL, idArr), importId + " - " + sheetLabel);

		return idSheet;
	}

	public void optClose(Object handler) throws Exception {
		if (null != handler) {
			DustCompApiUtils.setMsgVariant(CMD_PROCESSLINE, VT_TEXT, DustCompApiHacks.SAVE_END, true);
			DustCompApiUtils.setMsgValue(CMD_BLOCK_END, FLD_TARGET, handler, true);
		}
	}

	public Object write(String fileName, DustIdentifier idSheet, Object ob, Object handler) throws Exception {
		if (null == handler) {
			DustIdentifier handlerPath = idFact.combine(new DustIdentifier[] { FLD_SAVESTREAM, FLD_PATH });
			DustCompApiUtils.setValue(handlerPath, fileName);

			handler = DustCompApiUtils.getValue(FLD_SAVESTREAM);

			DustCompApiUtils.setMsgValue(CMD_BLOCK_START, FLD_TARGET, handler, true);

			DustCompApiUtils.setMsgValue(CMD_PROCESSLINE, FLD_TARGET, handler, false);
			DustCompApiUtils.setMsgVariant(CMD_PROCESSLINE, VT_TEXT, DustCompApiHacks.SAVE_START, true);

			// Object ot = DustCompApiUtils.getGlobalValue(TYPE_TYPE, idSheet);
			// DustCompApiUtils.setMsgVariant(CMD_PROCESS, VT_LINK, ot, true);

			String id = idSheet.toString();

			DustIdentifier dataPath = idFact.accessPath(DustIdentifier.ICTX_GLOBAL, new DustIdentifier[] { TYPE_STREAMINFO, idFact.smartId(id) });
			Object cFile = DustCompApiUtils.getGlobalValue(TYPE_STREAMINFO, idFact.smartId(id));

			Object path = DustCompApiUtils.getValue(DustIdentifier.ICTX_THIS, new DustIdentifier[] { FLD_SAVESTREAM, FLD_ABSOLUTEPATH });
			DustCompApiUtils.setValue(dataPath, TYPE_STREAMINFO, FLD_PATH, path);
			DustCompApiUtils.setValue(dataPath, TYPE_STREAMINFO, FLD_CTYPE, CONTENT_JSON);
			DustCompApiUtils.setValue(dataPath, TYPE_STREAMINFO, FLD_DATEUGLY, DustCompUtilsDateFormatter.dateToStr(DATE_SIMPLE));

			DustCompApiUtils.setValue(idFact.accessPath(DustIdentifier.ICTX_GLOBAL, new DustIdentifier[] { TYPE_TYPE, idSheet, FLD_SOURCE }), cFile);
		} else {
			DustCompApiUtils.setMsgVariant(CMD_PROCESSLINE, VT_TEXT, " , ", true);
		}

		DustCompApiUtils.setMsgValue(CMD_PROCESS, FLD_TARGET, DustCompApiUtils.getValue(FLD_SAVER), false);
		DustCompApiUtils.setMsgVariant(CMD_PROCESS, VT_LINK, ob, true);

		return handler;
	}

	DustIdentifier dataPath = DustCompApiUtils.ctxPath(DustIdentifier.ICTX_VISIT);
	Object vtText = DustCompApiUtils.getGlobalValue(TYPE_IDENTIFIED, VT_TEXT);
	Map<String, String[]> cmds = new HashMap<>();
	final String[] noval = {};

	Object geocoder;

	void setCmd(String val) {
		cmds.clear();

		if (null != val) {
			val = DustCompUtils.unescape(val, ESCAPECHARS);
			for (String cmd : val.split("!")) {
				if (!DustCompUtils.isEmpty(cmd)) {
					String[] cmdp = cmd.split(":");
					cmds.put(cmdp[0], (cmdp.length == 1) ? null : cmdp[1].split(","));
				}
			}
		}
	}

	boolean hasCmd(String cmd) {
		return cmds.containsKey(cmd.substring(1));
	}

	String[] getCmdPars(String cmd) {
		String[] ret = cmds.get(cmd.substring(1));
		return (null == ret) ? noval : ret;
	}

	String getCmdPar(String cmd) {
		String[] ret = cmds.get(cmd.substring(1));
		return (null == ret) ? null : ret[0];
	}
	
	DustIdentifier addField(DustIdentifier idFldInit, String id, final String colName) throws Exception {
		final String fldId = DustCompUtils.toId(colName);
		
		DustCompApiUtils.visitChildById(idFldInit, new DustValueVisitorDefault() {
			@Override
			protected void process(Object value, int idx) throws Exception {
				DustCompApiUtils.setValue(dataPath, TYPE_TEXT_HUMANINFO, FLD_LABEL, colName);
				DustCompApiUtils.setValue(dataPath, TYPE_FIELDDEF, FLD_VALUETYPE, vtText);
				DustCompApiUtils.setValue(dataPath, TYPE_IDENTIFIED, FLD_LOCALID, fldId);
			}
		}, id + "_" + fldId, TYPE_FIELDDEF);

		return idFact.smartId(fldId);
	}

	public void evaluate(String fileName) throws Exception {
		File f = new File(fileName);
		InputStream inStream = new FileInputStream(f);
		Workbook wb = fileName.toLowerCase().endsWith(".xlsx") ? new XSSFWorkbook(inStream) : new HSSFWorkbook(inStream);

		DustIdentifier[] idArr = new DustIdentifier[4];
		idArr[0] = TYPE_TYPE;

		ArrayList<DustIdentifier> arrRow = new ArrayList<>();

		DustIdentifier idPath = idFact.accessPath(DustIdentifier.ICTX_PARAM, new DustIdentifier[] { TYPE_VARIANT, VT_LINK, FLD_SOURCE, TYPE_IDENTIFIED, FLD_LOCALID });
		String id = (String) DustCompApiUtils.getValue(idPath);

		if (DustCompUtils.isEmpty(id)) {
			idPath = idFact.accessPath(DustIdentifier.ICTX_PARAM, new DustIdentifier[] { TYPE_VARIANT, VT_LINK, FLD_SOURCE, TYPE_IDENTIFIED, FLD_ID });
			id = (String) DustCompApiUtils.getValue(idPath);
		}

		Map<String, Object> calcColValues = new HashMap<>();
		Map<DustIdentifier, Object> rollColValues = new HashMap<>();
		Set<Map<DustIdentifier, Object>> skipRowData = new HashSet<>();

		try {
			int sn = wb.getNumberOfSheets();

			System.out.println("Reading workbook " + fileName + ": " + sn + " sheets...");

			for (int is = 0; is < sn; ++is) {
				Sheet sheet = wb.getSheetAt(is);
				String sheetName = sheet.getSheetName();

				DustIdentifier idSheet = initSheet(id, sheetName, idArr);

				Object writer = null;

				if (null == idSheet) {
					continue;
				}

				idArr[2] = TYPE_TYPE;
				idArr[3] = FLD_FIELDS;
				DustIdentifier idFldInit = idFact.accessPath(DustIdentifier.ICTX_GLOBAL, idArr);

				int rowCount = sheet.getLastRowNum();
				arrRow.clear();
				boolean titleRow = true;

				System.out.println(" Reading sheet " + sheetName + ": " + rowCount + " rows...");

				int ctrlCol = -2;
				calcColValues.clear();
				String toCol = null;
				setCmd(null);
				skipRowData.clear();
				geocoder = null;

				for (int ir = 0; ir < rowCount; ++ir) {
					Row row = sheet.getRow(ir);

					if (null == row) {
						continue;
					}

					// int rowNum = row.getRowNum() + 1;
					boolean rowValid = false;

					int cc = row.getLastCellNum();

					if (-2 == ctrlCol) {
						ctrlCol = -1;
						boolean readColNamesNow = false;

						for (int ic = 0; ic < cc; ++ic) {
							Cell cell = row.getCell(ic);
							if (null != cell) {
								String val = cell.toString();
								if (val.startsWith(CMD_CTRL)) {
									ctrlCol = ic;
									setCmd(val);

									readColNamesNow = hasCmd(CMD_COLNAMES);
									for (String colName : getCmdPars(CMD_CALCCOL)) {
										addField(idFldInit, id, colName);
									}

									for (String colName : getCmdPars(CMD_ROLLCOL)) {
										rollColValues.put(idFact.smartId(colName), null);
									}

									if (hasCmd(CMD_GEOCODE)) {
										geocoder = DustCompApiUtils.getValue(FLD_GEOCODER);
										if (null != geocoder) {
											DustCompApiUtils.chgMsgValue(CMD_INIT, DustCompGeocoderNames.FLD_CONFIG, null, DustChangeCommand.CLEAR);
											for (String gc : getCmdPars(CMD_GEOCODE)) {
												DustCompApiUtils.chgMsgValue(CMD_INIT, DustCompGeocoderNames.FLD_CONFIG, gc, DustChangeCommand.INSERT);

												String[] spl = gc.split(DustCompGeocoderNames.FLDSEP);
												if (spl.length > 1) {
													if (!DustGeocoderNames.CFGFIELDS[0].equals(spl[0])) {
														addField(idFldInit, id, spl[1]);
													}
												}
											}
											DustCompApiUtils.setMsgValue(CMD_INIT, FLD_TARGET, geocoder, true);
										}
									}

									break;
								}
							}
						}

						if ((-1 < ctrlCol) && !readColNamesNow) {
							titleRow = false;
							continue;
						}
					} else if (-1 < ctrlCol) {
						setCmd(DustCompUtils.safeToString(row.getCell(ctrlCol)));

						if (hasCmd(CMD_SKIP)) {
							continue;
						}

						toCol = getCmdPar(CMD_TOCOL);
						for (String colName : getCmdPars(CMD_RESETCOL)) {
							calcColValues.put(colName, null);
						}
					}

					if (hasCmd(CMD_COLNAMES)) {
						titleRow = true;
					}

					for (int ic = 0; ic < cc; ++ic) {
						Cell cell = row.getCell(ic);
						if (null == cell) {
							if (titleRow) {
								arrRow.add(null);
							}
							continue;
						}

						// int colNum = cell.getColumnIndex() + 1;
						// String colName = DustCompUtils.intToColumnLetter(colNum, false,
						// 2);

						Object val = null;
						// DustIdentifier varType = null;
						// String formula = null;

						int poiType = cell.getCellType();

						if (Cell.CELL_TYPE_FORMULA == poiType) {
							// formula = cell.getCellFormula();
							poiType = cell.getCachedFormulaResultType();
						}

						switch (poiType) {
						case Cell.CELL_TYPE_STRING:
							// varType = VT_TEXT;
							val = cell.getStringCellValue();
							break;
						case Cell.CELL_TYPE_NUMERIC:
							// varType = VT_REAL;
							val = cell.getNumericCellValue();
							break;
						case Cell.CELL_TYPE_BOOLEAN:
							// varType = VT_BOOL;
							val = cell.getBooleanCellValue();
							break;
						}

						// varType = VT_TEXT;

						if (titleRow) {
							final String fldId = DustCompUtils.safeToString(val);

							if (DustCompUtils.isEmpty(fldId)) {
								arrRow.add(null);
							} else {
								if ((0 > ctrlCol) || (ic < ctrlCol)) {
									arrRow.add(addField(idFldInit, id, fldId));
								}
							}
						} else {
							if (null != toCol) {
								if (!DustCompUtils.isEmpty((String) val) && (ic < ctrlCol)) {
									calcColValues.put(toCol, val);
								}
							} else if (ic < arrRow.size()) {
								DustIdentifier fi = (DustIdentifier) arrRow.get(ic);
								if (null != fi) {
									DustCompApiUtils.setValue(FLD_WORKENTITY, idSheet, fi, val);
									rowValid |= (null != val);
								}
							}
						}
					}

					if (titleRow) {
						DustCompApiUtils.changeValue(FLD_WORKENTITY, DustChangeCommand.REPLACE, DustCompApiUtils.getTempValue(idSheet), 0);
						titleRow = false;
					} else {

						if (-1 < ctrlCol) {
							if (hasCmd(CMD_TOCOL)) {
								rowValid = false;
							} else if (hasCmd(CMD_SKIPALL)) {
								Map<DustIdentifier, Object> skipData = new HashMap<>();
								for (DustIdentifier fi : arrRow) {
									skipData.put(fi, DustCompApiUtils.getValue(FLD_WORKENTITY, idSheet, fi));
								}
								skipRowData.add(skipData);
								rowValid = false;
							} else {
								for (Map<DustIdentifier, Object> skipData : skipRowData) {
									boolean match = true;
									for (Map.Entry<DustIdentifier, Object> e : skipData.entrySet()) {
										if (!DustCompUtils.isEqual(e.getValue(), DustCompApiUtils.getValue(FLD_WORKENTITY, idSheet, e.getKey()))) {
											match = false;
											break;
										}
									}
									if (match) {
										rowValid = false;
										break;
									}
								}
							}
						}

						Object we = DustCompApiUtils.getValue(FLD_WORKENTITY);
						if (rowValid) {
							for (Map.Entry<String, Object> e : calcColValues.entrySet()) {
								DustCompApiUtils.setValue(FLD_WORKENTITY, idSheet, idFact.smartId(e.getKey()), e.getValue());
							}
							
							for (Map.Entry<DustIdentifier, Object> e : rollColValues.entrySet()) {
								DustIdentifier ii = e.getKey();
								Object val = DustCompApiUtils.getValue(FLD_WORKENTITY, idSheet, ii);
								Object rVal = e.getValue();
								
								if ( null == val ) {
									val = rVal;
								} else {
									rollColValues.put(e.getKey(), val);
								}
								DustCompApiUtils.setValue(FLD_WORKENTITY, idSheet, ii, val);
							}

							if (null != geocoder) {
								DustCompApiUtils.setMsgValue(CMD_PROCESS, FLD_TARGET, geocoder, false);
								DustCompApiUtils.setMsgVariant(CMD_PROCESS, VT_LINK, we, true);
							}
							writer = write(id + "_" + sheetName + ".json", idSheet, we, writer);
						}
					}
				}

				System.out.println(" Reading sheet ended.");

				optClose(writer);

			}
		} finally

		{
			wb.close();
		}
	}
}
