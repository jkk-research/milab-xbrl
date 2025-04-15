package org.xbrldock.vsme.poc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

import org.jsoup.nodes.Element;

import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class VsmeDataGrid implements VsmePocConsts, XbrlDockUtilsGui.WidgetContainer {
	private static Icon iDel = VsmeUtils.getCmdIcon(XDC_CMD_GEN_DELETE);

	EditContext ctx;

	String attId;
	Map<String, Object> attDef;

	ArrayList<Map> atts;
	ArrayList<Map> rowDefs;
	ArrayList<Map> calcDefs = new ArrayList<>();
	ArrayList<Map<String, Object>> values;
	Object editor;

	boolean editRows = true;

	class GridModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		public GridModel() {

			values = XbrlDockUtils.simpleGet(ctx.getReport(), attId);
			if (null == values) {
				values = new ArrayList<>();
			}

			atts = XbrlDockUtils.simpleGet(attDef, XDC_GEN_TOKEN_attributes);

			if (null == atts) {
				String src = XbrlDockUtils.simpleGet(attDef, XDC_GEN_TOKEN_source);
				atts = XbrlDockUtils.simpleGet(ctx.getMeta(), "tables", src, XDC_GEN_TOKEN_attributes);
			}

			for (Map<String, Object> d : atts) {
				optAddCalc(d);
			}

			rowDefs = XbrlDockUtils.simpleGet(attDef, XDC_GEN_TOKEN_rows);
			Map<String, Object> rowSource = XbrlDockUtils.simpleGet(attDef, VSME_rowSource);

			if ((null != rowDefs) || (null != rowSource)) {
				editRows = false;

				if (!ctx.getReport().containsKey(attId)) {
					if (null != rowDefs) {
						for (Map<String, Object> rd : rowDefs) {
							TreeMap<String, Object> em = new TreeMap<String, Object>();
							values.add(em);
							em.put(XDC_EXT_TOKEN_id, XbrlDockUtils.simpleGet(rd, XDC_EXT_TOKEN_id));
							optAddCalc(rd);
						}
					} else if (null != rowSource) {
						if (XbrlDockUtils.isEqual(VSME_standard, XbrlDockUtils.simpleGet(rowSource, XDC_GEN_TOKEN_source))) {
							Map<String, String> fill = XbrlDockUtils.simpleGet(rowSource, VSME_fill);

							Element e = ctx.getStandard().getElementById(XbrlDockUtils.simpleGet(rowSource, XDC_EXT_TOKEN_id));
							for (Iterator<Element> eit = e.children().iterator(); eit.hasNext();) {
								Element ei = eit.next();
								TreeMap<String, Object> em = new TreeMap<String, Object>();
								values.add(em);

								for (Map.Entry<String, String> fe : fill.entrySet()) {
									em.put(fe.getKey(), ei.attr(fe.getValue()));
								}
							}
						}
					}

					ctx.getReport().put(attId, values);
				}
			}

			editor = XbrlDockUtils.simpleGet(attDef, XDC_GEN_TOKEN_editor);
		}

		public void optAddCalc(Map<String, Object> d) {
			Map<String, Object> cd = XbrlDockUtils.simpleGet(d, VSME_calc);
			if (null != cd) {
				calcDefs.add(cd);
				cd.put(XDC_EXT_TOKEN_id, d.get(XDC_EXT_TOKEN_id));
			}
		}

		@Override
		public int getColumnCount() {
			return editRows ? (atts.size() + 1) : atts.size();
		}

		@Override
		public String getColumnName(int column) {
			return (atts.size() > column) ? (String) atts.get(column).get(XDC_EXT_TOKEN_id) : "Del";
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == atts.size()) {
				return Icon.class;
			}

			String type = (String) atts.get(columnIndex).get(XDC_EXT_TOKEN_type);

			switch (type) {
			case "Boolean":
				return Boolean.class;
			case "Real":
				return Double.class;
			}

			return Object.class;
		}

		@Override
		public int getRowCount() {
			return editRows ? (values.size() + 1) : values.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (rowIndex == values.size()) {
				return null;
			}

			if (columnIndex == atts.size()) {
				return iDel;
			}

			return values.get(rowIndex).get(atts.get(columnIndex).get(XDC_EXT_TOKEN_id));
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (null != editor) {
//				return false;
			}
			if (!editRows && (0 == columnIndex)) {
				return false;
			}
			return atts.size() > columnIndex;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (rowIndex == values.size()) {
				values.add(new TreeMap<String, Object>());

				if (0 == rowIndex) {
					ctx.getReport().put(attId, values);
				}

				fireTableRowsInserted(rowIndex, rowIndex);
			}

			String colId = (String) atts.get(columnIndex).get(XDC_EXT_TOKEN_id);
			ctx.setReportValue(aValue, attId, rowIndex, colId);

			if (!calcDefs.isEmpty()) {
				Set<String> updatedCols = new TreeSet<>();
				for (Map<String, Object> cd : calcDefs) {
					executeCD(cd, rowIndex, colId);

					if (XbrlDockUtils.isEqual(XDC_GEN_TOKEN_row, cd.get(XDC_GEN_TOKEN_source))) {
						updatedCols.add((String) cd.get(XDC_EXT_TOKEN_id));
					}
				}

				for (String ccol : updatedCols) {
					for (Map<String, Object> cdc : calcDefs) {
						if (XbrlDockUtils.isEqual(XDC_GEN_TOKEN_col, cdc.get(XDC_GEN_TOKEN_source))) {
							executeCD(cdc, rowIndex, ccol);
						}
					}
				}

				fireTableDataChanged();
			}
		}

		public void executeCD(Map<String, Object> cd, int rowIndex, String colId) {
			ArrayList<Object> data = new ArrayList<>();

			String cid = (String) cd.get(XDC_EXT_TOKEN_id);
			int ri = rowIndex;

			String s = (String) cd.get(XDC_GEN_TOKEN_source);
			switch (s) {
			case XDC_GEN_TOKEN_row:
				for (Map.Entry<String, Object> ve : values.get(rowIndex).entrySet()) {
					if (XbrlDockUtils.isEqual(cid, ve.getKey())) {
						continue;
					}
					data.add(ve.getValue());
				}
				break;
			case XDC_GEN_TOKEN_col:
				int cr = 0;
				for (Map<String, Object> r : values) {
					if (XbrlDockUtils.isEqual(cid, r.get(XDC_EXT_TOKEN_id))) {
						ri = cr;
						continue;
					}
					++cr;
					data.add(r.get(colId));
				}
				cid = colId;
				break;
			}

			s = (String) cd.get(XDC_GEN_TOKEN_method);

			double aggVal = 0.0;

			switch (s) {
			case XDC_GEN_TOKEN_sum:
				for (Object n : data) {
					try {
						aggVal += Double.parseDouble(XbrlDockUtils.toString(n));
					} catch (Throwable t) {

					}
				}
				break;
			}

			ctx.setReportValue(aggVal, attId, ri, cid);
		}

		public void updateValue(Object value) {
			if (null == value) {
				values.clear();
			} else {
				values = (ArrayList<Map<String, Object>>) value;
			}

			fireTableDataChanged();
		}
	}

	MouseAdapter clickListener = new MouseAdapter() {
		@Override
		public void mouseClicked(java.awt.event.MouseEvent evt) {
//			if (protAtts.contains(attId)) {
//				return;
//			}

			JTable tbl = (JTable) evt.getSource();
			int col = tbl.columnAtPoint(evt.getPoint());
			int row = tbl.rowAtPoint(evt.getPoint());

			if (atts.size() == col) {
				if ((0 <= row) && (row < values.size())) {
					values.remove(row);

					if (values.isEmpty()) {
						ctx.getReport().remove(attId);
					}

					mdl.fireTableRowsDeleted(row, row);
				}
			} else if (null != editor) {
				Map<String, Object> p = new TreeMap<>();

				p.put(XDC_EXT_TOKEN_id, attId);
				p.put(XDC_GEN_TOKEN_row, row);
				p.put(XDC_GEN_TOKEN_col, atts.get(col));
				ctx.activateEditor(editor, p);
			}
		}
	};

	JScrollPane scp;
	JTable tbl;
	GridModel mdl;

	GenAgent listener;

	public VsmeDataGrid(EditContext ctx, String attId, Map<String, Object> attDef) {
		this.ctx = ctx;
		this.attId = attId;
		this.attDef = attDef;

		mdl = new GridModel();

		tbl = new JTable(mdl);
		tbl.addMouseListener(clickListener);

		tbl.setGridColor(Color.darkGray);
		JTableHeader header = tbl.getTableHeader();
		header.setBackground(Color.LIGHT_GRAY);
		header.setForeground(Color.blue);

		scp = new JScrollPane(tbl);
		if (editRows) {
			scp.setPreferredSize(new Dimension(300, 100));
		}

		XbrlDockUtilsGui.setTitle(scp, attId);
	}

	@Override
	public JComponent getWidget() {
		return scp;
	}
	
	@Override
	public void setGuiValue(Object value) {
		mdl.updateValue(value);
	}

	public JTable getTable() {
		return tbl;
	}
}
