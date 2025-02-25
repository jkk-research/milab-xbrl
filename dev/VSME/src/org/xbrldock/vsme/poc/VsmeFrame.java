package org.xbrldock.vsme.poc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.gui.XbrlDockGuiUtilsHtmlDisplay;
import com.xbrldock.poc.gui.XbrlDockGuiWidgetGrid;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;
import com.xbrldock.utils.XbrlDockUtilsJson;
import com.xbrldock.utils.XbrlDockUtilsMvel;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class VsmeFrame implements VsmePocConsts, XbrlDockConsts.GenAgent {

	class AttGridModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		String attId;
		ArrayList<Map> atts;
		ArrayList<Map<String, Object>> values;

		MouseAdapter rowRemover = new MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				JTable tbl = (JTable) evt.getSource();
				int col = tbl.columnAtPoint(evt.getPoint());
				if (atts.size() == col) {
					int row = tbl.rowAtPoint(evt.getPoint());
					if ((0 <= row) && (row < values.size())) {
						values.remove(row);

						if (values.isEmpty()) {
							report.remove(attId);
						}

						fireTableRowsDeleted(row, row);
					}
				}
			}
		};

		public AttGridModel(String attId, Map<String, Object> attDef) {
			this.attId = attId;

			values = XbrlDockUtils.simpleGet(report, attId);
			if (null == values) {
				values = new ArrayList<>();
			}

			atts = XbrlDockUtils.simpleGet(attDef, VSME_attributes);

			if (null == atts) {
				String src = XbrlDockUtils.simpleGet(attDef, XDC_GEN_TOKEN_source);
				atts = XbrlDockUtils.simpleGet(meta, "tables", src, VSME_attributes);
			}
		}

		@Override
		public int getColumnCount() {
			return atts.size() + 1;
		}

		@Override
		public String getColumnName(int column) {
			return (atts.size() > column) ? (String) atts.get(column).get(XDC_EXT_TOKEN_id) : "Del";
		}

		@Override
		public int getRowCount() {
			return values.size() + 1;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (rowIndex == values.size()) {
				return null;
			}

			if (columnIndex == atts.size()) {
				return "X";
			}

			return values.get(rowIndex).get(atts.get(columnIndex).get(XDC_EXT_TOKEN_id));
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return atts.size() > columnIndex;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (rowIndex == values.size()) {
				values.add(new TreeMap<String, Object>());

				if (0 == rowIndex) {
					report.put(attId, values);
				}

				fireTableRowsInserted(rowIndex, rowIndex);
			}

			values.get(rowIndex).put((String) atts.get(columnIndex).get(XDC_EXT_TOKEN_id), aValue);
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

	Document standard;
	Map<String, Object> meta;

	Map<String, Object> panelMap;
	Map<String, Element> standardElements;

	Map<String, Object> report;
	ArrayList<Map<String, String>> messages;

	JFrame frame;
	XbrlDockGuiUtilsHtmlDisplay htmlStandard;
	XbrlDockGuiUtilsHtmlDisplay htmlHelp;
	Map<String, Object> widgets = new TreeMap<>();

	JComboBox<String> cbPageSelector;
	JPanel pnlValueEditor;
	JPanel pnlReportPage;

	XbrlDockGuiWidgetGrid gridMsg;

	JFileChooser fc;

	@Override
	public Object process(String cmd, Map<String, Object> params) throws Exception {
		switch (cmd) {
		case XDC_CMD_GEN_Init:
			frame = new JFrame((String) XbrlDockUtils.simpleGet(params, XDC_EXT_TOKEN_name));
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			Container cp = frame.getContentPane();

			htmlStandard = new XbrlDockGuiUtilsHtmlDisplay();
			htmlStandard.setPlaceholder("Here comes the whole standard");
			htmlHelp = new XbrlDockGuiUtilsHtmlDisplay();
			htmlHelp.setPlaceholder("Here comes the extract related to the selected item");

			JTabbedPane tpHtml = new JTabbedPane();
			tpHtml.add("Help", htmlHelp.getComp());
			tpHtml.add("Standard", htmlStandard.getComp());

			pnlValueEditor = new JPanel(new BorderLayout());
			pnlValueEditor.add(new JLabel("here comes the item selector"), BorderLayout.CENTER);

			JComponent cpRight = XbrlDockUtilsGui.createSplit(false, tpHtml, new JScrollPane(pnlValueEditor), 0.2);

			JPanel pnlReport = new JPanel(new BorderLayout());

			cbPageSelector = new JComboBox<String>();

			pnlReportPage = new JPanel(new GridBagLayout());

			pnlReport.add(cbPageSelector, BorderLayout.NORTH);
			pnlReport.add(new JScrollPane(pnlReportPage), BorderLayout.CENTER);

			fc = new JFileChooser(new File("."));
			fc.setFileFilter(new FileNameExtensionFilter("JSON meta files", "json"));

			JPanel pnlCmds = new JPanel(new FlowLayout(FlowLayout.CENTER));
			pnlCmds.add(XbrlDockUtilsGui.createBtn(XDC_CMD_GEN_LOAD, alCmd, JButton.class));
			pnlCmds.add(XbrlDockUtilsGui.createBtn(XDC_CMD_GEN_SAVE, alCmd, JButton.class));

			messages = new ArrayList<>();
			gridMsg = new XbrlDockGuiWidgetGrid(new LabeledAccess("Level", VSME_msgLevel), new LabeledAccess("Location", VSME_msgRef),
					new LabeledAccess("Source", VSME_msgExpr), new LabeledAccess("Message", XDC_EXT_TOKEN_value));

			JComponent cpLeft = XbrlDockUtilsGui.createSplit(false, pnlReport, gridMsg.getComp(), 0.8);
			cp.add(XbrlDockUtilsGui.createSplit(true, cpLeft, cpRight, 0.8), BorderLayout.CENTER);
			cp.add(pnlCmds, BorderLayout.SOUTH);

			break;
		case XDC_CMD_GEN_Begin:
			meta = XbrlDockUtils.simpleGet(params, VSME_meta);
			standard = XbrlDockUtils.simpleGet(params, VSME_standard);
			readStandard();

			panelMap = new TreeMap<String, Object>();
			Collection<Map<String, Object>> panels = XbrlDockUtils.simpleGet(meta, VSME_panels);
			for (Map<String, Object> p : panels) {
				String id = XbrlDockUtils.simpleGet(p, XDC_EXT_TOKEN_id);
				panelMap.put(id, p);
			}

			report = new TreeMap<String, Object>();

			setReportPage(XbrlDockUtils.simpleGet(meta, VSME_start));

			frame.pack();

			frame.setVisible(true);

			break;
		}
		return null;
	}

	void readStandard() {
		Elements nl = standard.getAllElements();

		standardElements = new TreeMap<>();

		for (Element e : nl) {
			String id = e.attr(XDC_EXT_TOKEN_id);

			String att = e.attr(XDC_EXT_TOKEN_class);

			if ((null != att) && att.contains("vmes_page")) {
				cbPageSelector.addItem(e.text());
			}

			if (!XbrlDockUtils.isEmpty(id)) {
				standardElements.put(id, e);
			}

		}
	}

	FocusAdapter fa = new FocusAdapter() {
		@Override
		public void focusGained(FocusEvent e) {
			Object id = ((JComponent) e.getSource()).getClientProperty(XDC_EXT_TOKEN_id);
			String help = "";
			if (null != id) {
				Element se = standardElements.get(id);

				if (null != se) {
					help = se.outerHtml();
				}
			}

			htmlHelp.setText(help);
		}
	};

	DocumentListener dl = new DocumentListener() {
		JTextField fld;
		String value;

		Runnable resetValue = new Runnable() {
			@Override
			public void run() {
				fld.setText(value);
			}
		};

		@Override
		public void removeUpdate(DocumentEvent e) {
			updateValue(e);
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			updateValue(e);
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			updateValue(e);
		}

		void updateValue(DocumentEvent e) {
			try {
				javax.swing.text.Document doc = e.getDocument();
				String id = (String) doc.getProperty(XDC_EXT_TOKEN_id);

				if (null != id) {
					String s = doc.getText(0, doc.getLength());

					if (XbrlDockUtils.isEmpty(s)) {
						setReportValue(null, id);
					} else {
						Object val = s;

						try {
							switch ((String) doc.getProperty(XDC_EXT_TOKEN_type)) {
							case "Real":
								val = Double.parseDouble(s);
								break;
							case "Int":
								val = Long.parseLong(s);
								break;
							}
						} catch (Throwable t) {
							value = XbrlDockUtils.toString(report.get(id));
							fld = (JTextField) doc.getProperty(XDC_CFGTOKEN_gui);
							java.awt.Toolkit.getDefaultToolkit().beep();
							SwingUtilities.invokeLater(resetValue);
						}

						setReportValue(val, id);
					}
				}
			} catch (Throwable e1) {
				XbrlDockException.wrap(e1);
			}

		}
	};

	ActionListener alCmd = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			int fcRet;

			try {
				switch (cmd) {
				case XDC_CMD_GEN_SAVE:
					fcRet = fc.showSaveDialog(frame);
					if (fcRet == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						XbrlDockUtilsJson.writeJson(file, report);
					}
					break;
				case XDC_CMD_GEN_LOAD:
					fcRet = fc.showOpenDialog(frame);

					if (fcRet == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						report = XbrlDockUtilsJson.readJson(file);

						for (Map.Entry<String, Object> re : report.entrySet()) {
							setGuiValue(re.getValue(), re.getKey());
						}
					}
					break;
				}
			} catch (Throwable t) {
				XbrlDockException.wrap(t, "Command", cmd);
			}
		};
	};

	ActionListener al = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			JComponent comp = (JComponent) e.getSource();

			if (comp instanceof AbstractButton) {
				AbstractButton abtn = (AbstractButton) comp;
				if (abtn.isSelected()) {
					setReportValue(abtn.getActionCommand(), (String) abtn.getClientProperty(XDC_EXT_TOKEN_id));
				}
			}
		}
	};

	private void setGuiValue(Object value, String attId) {
		Object w = widgets.get(attId);

		if (w instanceof JTextField) {
			((JTextField) w).setText(XbrlDockUtils.toString(value));
		} else if (w instanceof ButtonGroup) {
			for (Enumeration<AbstractButton> eb = ((ButtonGroup) w).getElements(); eb.hasMoreElements();) {
				AbstractButton ab = eb.nextElement();
				ab.setSelected(XbrlDockUtils.isEqual(value, ab.getActionCommand()));
			}
		} else if (w instanceof JTable) {
			AttGridModel agm = (AttGridModel) ((JTable) w).getModel();
			agm.updateValue(value);
		}
	};

	private Object setReportValue(Object value, String attId) {
		return setReportValue(value, attId, -1, null);
	};

	private Object setReportValue(Object value, String attId, int row, String col) {
		Object orig = report.get(attId);

		if (null == value) {
			report.remove(attId);
			if (null != orig) {
				reportUpdated(value, attId);
			}
		} else {
			if (!XbrlDockUtils.isEqual(value, orig)) {
				report.put(attId, value);
				reportUpdated(value, attId);
			}
		}

		return orig;
	};

	private void reportUpdated(Object value, String attId) {
		reportUpdated(value, attId, -1, null);
	};

	private void reportUpdated(Object value, String attId, int row, String col) {
		Collection<Map<String, Object>> expressions = (Collection<Map<String, Object>>) meta.get(VSME_expressions);

		ExprCtx ectx = null;

		for (Map<String, Object> e : expressions) {
			Collection<String> ei = (Collection<String>) e.get(VSME_items);

			if (ei.contains(attId)) {
				String expr = (String) e.get(XDC_FORMULA_formula);

				if (!XbrlDockUtils.isEmpty(expr)) {
					String tn = (String) e.get(XDC_GEN_TOKEN_target);
					Object target = XbrlDockUtils.simpleGet(meta, VSME_attributes, tn);

					if (null == ectx) {
						ectx = new ExprCtx();
						ectx.data.put("report", report);
						ectx.data.put("ectx", ectx);
					}

					ectx.exprId = (String) e.get(XDC_EXT_TOKEN_id);

					if (XbrlDockUtils.isEmpty(ectx.exprId)) {
						ectx.exprId = tn;
					}

					ectx.data.put(XDC_GEN_TOKEN_target, target);

					Object result;
//					result = VsmeTest.categoryExpr(ctx);
					Object comp = XbrlDockUtilsMvel.compile(expr);
					result = XbrlDockUtilsMvel.evalCompiled(comp, ectx, ectx.data);

					if (XbrlDockUtils.isEqual(result, -1)) {
						XbrlDock.log(EventLevel.Error, "Invalid category, above Medium level");
					} else {
						setReportValue(result, tn);
						XbrlDock.log(EventLevel.Trace, "Setting attribute", tn, "to", result);
						setGuiValue(result, tn);
					}
				}
			}
		}
	}

	private void setReportPage(String pageId) {
		Map<String, Object> page = XbrlDockUtils.simpleGet(panelMap, pageId);

		pnlReportPage.removeAll();

		Collection<String> items = XbrlDockUtils.simpleGet(page, VSME_items);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 2, 2, 2);

		for (String i : items) {
			XbrlDockUtilsGui.nextGBRow(gbc, true);
			JLabel label = new JLabel(i);

			Map<String, Object> attDef = XbrlDockUtils.simpleGet(meta, VSME_attributes, i);
			String attType = XbrlDockUtils.simpleGet(attDef, XDC_EXT_TOKEN_type);

			JComponent comp = null;
			switch (attType) {
			case "String":
			case "Real":
				comp = createTextField(i, attType, attDef);
				break;
			case "Identifier":
				comp = createRadio(i, attDef);
				break;
			case "Table":
				comp = createGrid(i, attDef);
				label = null;
				break;
			}

			if (null != label) {
				Element se = standardElements.get(i);
				if (null != se) {
					label.setToolTipText(se.text());
				}
				pnlReportPage.add(label, gbc);
				XbrlDockUtilsGui.nextGBCell(gbc, true);
			} else {
				gbc.gridwidth = 2;
			}

			if (null == comp) {
				comp = new JLabel(attType);
			}

			gbc.weightx = 1.0;

			pnlReportPage.add(comp, gbc);
		}

		pnlReportPage.invalidate();
		pnlReportPage.getParent().revalidate();
	}

	public JComponent createGrid(String i, Map<String, Object> attDef) {
//		Collection<Map> atts = XbrlDockUtils.simpleGet(attDef, VSME_attributes);
//
//		if (null == atts) {
//			String src = XbrlDockUtils.simpleGet(attDef, XDC_GEN_TOKEN_source);
//			atts = XbrlDockUtils.simpleGet(meta, "tables", src, VSME_attributes);
//		}
//
//		DefaultTableModel tm = new DefaultTableModel(4, 0) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public boolean isCellEditable(int row, int column) {
//				return true;
//			}
//		};
//
//		for (Map a : atts) {
//			tm.addColumn(a.get(XDC_EXT_TOKEN_id));
//		}
//
//		JTable tbl = new JTable(tm);
		AttGridModel agm = new AttGridModel(i, attDef);
		JTable tbl = new JTable(agm);
		tbl.addMouseListener(agm.rowRemover);

		tbl.setGridColor(Color.darkGray);
		JTableHeader header = tbl.getTableHeader();
		header.setBackground(Color.LIGHT_GRAY);
		header.setForeground(Color.blue);

		JScrollPane scp = new JScrollPane(tbl);
		scp.setPreferredSize(new Dimension(600, 100));

		XbrlDockUtilsGui.setTitle(scp, i);

		tbl.addFocusListener(fa);

		widgets.put(i, tbl);

		return scp;
	}

	public JComponent createRadio(String i, Map<String, Object> attDef) {
		Collection<Object> options = XbrlDockUtils.simpleGet(attDef, VSME_options);

		if (null != options) {
			JPanel ret = new JPanel(new GridLayout(options.size(), 1));

			ButtonGroup bgr = new ButtonGroup();
			widgets.put(i, bgr);

			for (Object o : options) {
				String id = XbrlDockUtils.simpleGet(o, XDC_EXT_TOKEN_id);
				JRadioButton bt = new JRadioButton(id);
				bt.putClientProperty(XDC_EXT_TOKEN_id, i);
				bt.setActionCommand(id);
				bt.addActionListener(al);
				bgr.add(bt);
				ret.add(bt);
				widgets.put(id, bt);
			}

			return ret;
		}

		return null;
	}

	public JComponent createTextField(String i, String attType, Map<String, Object> attDef) {
		JTextField tf = new JTextField();

		switch (attType) {
		case "Real":
		case "Int":
			tf.setHorizontalAlignment(JTextField.RIGHT);
			break;
		}

		tf.putClientProperty(XDC_EXT_TOKEN_id, i);
		tf.addFocusListener(fa);

		if (XbrlDockUtils.checkFlag(attDef, "Calculated")) {
			tf.setEditable(false);
		} else {
			javax.swing.text.Document doc = tf.getDocument();
			doc.addDocumentListener(dl);
			doc.putProperty(XDC_EXT_TOKEN_id, i);
			doc.putProperty(XDC_EXT_TOKEN_type, attType);
			doc.putProperty(XDC_CFGTOKEN_gui, tf);
		}

		widgets.put(i, tf);
		return tf;
	}

}
