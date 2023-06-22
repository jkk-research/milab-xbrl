package com.gollywolly.dustcomp.dust.io.poi;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.gollywolly.dustcomp.DustCompServices.DustUtilsExplorer;
import com.gollywolly.dustcomp.DustCompServices.DustUtilsStateful;
import com.gollywolly.dustcomp.DustCompUtils;
import com.gollywolly.dustcomp.api.DustCompApiHacks;
import com.gollywolly.dustcomp.api.DustCompApiRuntimeException;
import com.gollywolly.dustcomp.api.DustCompApiUtils;
import com.gollywolly.dustcomp.coll.DustCompCollections;
import com.gollywolly.dustcomp.shared.DustCompSerializeNames;

public class DustCompPoiExportTemplate implements DustUtilsStateful, DustUtilsExplorer, DustCompPoiComponents, DustCompCollections, DustCompSerializeNames {

	abstract class Operation {
		Object source;
		CellReference target;
		EnumSet<TemplateOpFlag> flags = NOFLAGS;

		public Operation() {
			String s = (String) DustCompApiUtils.getValue(DustIdentifier.ICTX_VISIT, FLD_OPFLAGS);
			if (!DustCompUtils.isEmpty(s)) {
				flags = EnumSet.noneOf(TemplateOpFlag.class);
				for (String f : s.split(",")) {
					flags.add(TemplateOpFlag.valueOf(f));
				}
			}

			source = DustCompApiUtils.getValue(DustIdentifier.ICTX_VISIT, FLD_OPSOURCE);
			s = (String) DustCompApiUtils.getValue(DustIdentifier.ICTX_VISIT, FLD_OPTARGET);
			target = new CellReference(s);
		}

		Cell getCell() {
			int ri = target.getRow();
			Row r = (IDX_UNSET == ri) ? row : sheet.getRow(ri);
			return (null == r) ? null : r.getCell(target.getCol());
		}

		abstract void exec() throws Exception;
	}

	class OperationCopy extends Operation {
		TemplateCopyType ct;
		
		public OperationCopy() {
			String ctype = (String) DustCompApiUtils.getValue(DustIdentifier.ICTX_VISIT, FLD_OPCOPYTYPE);
			ct =  (DustCompUtils.isEmpty(ctype)) ? TemplateCopyType.string : TemplateCopyType.valueOf(ctype);
		}
		
		@Override
		void exec() throws Exception {
			Cell c = getCell();
			String sVal = content.get(source);

			if ( null == c ) {
//				System.out.println("WARNING: Skip copying " + source + " = " + sVal + " to " + target.formatAsString());
				return;
			}

			switch ( ct ) {
			case numeric:
				c.setCellValue(new Double(sVal));
				break;
			case string:
				c.setCellValue(sVal);
				break;
			}
			
		}
	}

	class OperationLocateRow extends Operation {
		int start, end;

		public OperationLocateRow() {
			start = DustCompApiHacks.getInt(DustIdentifier.ICTX_VISIT, FLD_OPFINDSTART, IDX_UNSET);
			end = DustCompApiHacks.getInt(DustIdentifier.ICTX_VISIT, FLD_OPFINDEND, IDX_UNSET);
		}

		@Override
		void exec() throws Exception {
			int s = (IDX_UNSET == start) ? sheet.getFirstRowNum() : start;
			int e = (IDX_UNSET == end) ? sheet.getLastRowNum() : end;
			Object val = content.get(source);

			for (int i = s; i < e; ++i) {
				row = sheet.getRow(i);
				String cv = getCell().toString();
				if (DustCompUtils.isEqual(val, cv)) {
					return;
				}
			}

			if (flags.contains(TemplateOpFlag.mustFind)) {
//				System.out.println("WARNING: Missing " + source + " in the order list: " + val);
			}
			
			row = null;
		}
	}
	
	
	String fldName;

	Workbook wb;
	Sheet sheet;
	Row row;

	Map<String, String> content = new HashMap<>();
	ArrayList<Operation> rowOps = new ArrayList<>();


	private Operation createOp() {
		String mode = (String) DustCompApiUtils.getValue(DustIdentifier.ICTX_VISIT, FLD_OPMODE);
		TemplateOp op = TemplateOp.valueOf(mode);

		switch (op) {
		case copy:
			return new OperationCopy();
		case locateRow:
			return new OperationLocateRow();
		}

		throw new DustCompApiRuntimeException("Unknown mode: " + mode);
	}

	@Override
	public void dust_utils_stateful_init() throws Exception {

		String fileName = (String) DustCompApiUtils.getValue(FLD_TEMPLATEPATH);
		wb = new XSSFWorkbook(fileName);

		String sheetName = (String) DustCompApiUtils.getValue(FLD_SHEETNAME);
		sheet = wb.getSheet(sheetName);

		DustCompApiUtils.visit(FLD_ROWOPS, new DustValueVisitorDefault() {
			@Override
			protected void process(Object value, int idx) throws Exception {
				rowOps.add(createOp());
			}
		});

		System.out.println("inited: " + fileName + " - " + sheetName);
	}

	@Override
	public void dust_utils_stateful_release() throws Exception {
		String targetfileName = (String) DustCompApiUtils.getValue(FLD_TARGETPATH);

		targetfileName = targetfileName.replace("%id%", String.valueOf(System.currentTimeMillis()));
		System.out.println("release: " + targetfileName);
		new File(targetfileName).getParentFile().mkdirs();

		wb.setForceFormulaRecalculation(true);
		wb.write(new FileOutputStream(targetfileName));
	}

	@Override
	public void dust_utils_visitor_startField() throws Exception {
		fldName = (String) DustCompApiUtils.getParamValue(FLD_ID);
	}

	@Override
	public void dust_utils_visitor_processValue() throws Exception {
		String val = (String) DustCompApiUtils.getParamValue(VAR_TEXT);
		content.put(fldName, val);
	}

	@Override
	public void dust_utils_visitor_endField() throws Exception {
		fldName = null;
	}

	@Override
	public void dust_utils_block_start() throws Exception {
		content.clear();
	}

	@Override
	public void dust_utils_block_end() throws Exception {
		for ( Operation op : rowOps ) {
			op.exec();
		}
		content.clear();
	}

}
