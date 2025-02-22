package org.xbrldock.vsme.poc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.NumberFormatter;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.gui.XbrlDockGuiUtilsHtmlDisplay;
import com.xbrldock.poc.gui.XbrlDockGuiWidgetGrid;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

public class VsmeFrame implements VsmePocConsts, XbrlDockConsts.GenAgent {

	Document standard;
	Map<String, Object> meta;

	Map<String, Object> panelMap;
	Map<String, Element> standardElements;

	Map<String, Object> report;
	ArrayList<Map<String, String>> messages;

	JFrame frame;
	XbrlDockGuiUtilsHtmlDisplay htmlStandard;
	XbrlDockGuiUtilsHtmlDisplay htmlHelp;

	JComboBox<String> cbPageSelector;
	JPanel pnlValueEditor;
	JPanel pnlReportPage;

	NumberFormatter fmtInt = new NumberFormatter(NumberFormat.getInstance());
	NumberFormatter fmtReal = new NumberFormatter(NumberFormat.getInstance());

	XbrlDockGuiWidgetGrid gridMsg;

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
//			fmtInt.setValueClass(Integer.class);
//			fmtInt.setAllowsInvalid(false);
//			fmtInt.setCommitsOnValidEdit(true);
//			fmtReal.setValueClass(Double.class);
//			fmtReal.setAllowsInvalid(false);
//			fmtReal.setCommitsOnValidEdit(true);

			pnlReport.add(cbPageSelector, BorderLayout.NORTH);
			pnlReport.add(new JScrollPane(pnlReportPage), BorderLayout.CENTER);

			messages = new ArrayList<>();
			gridMsg = new XbrlDockGuiWidgetGrid(new LabeledAccess("Level", VSME_msgLevel), new LabeledAccess("Location", VSME_msgRef),
					new LabeledAccess("Source", VSME_msgExpr), new LabeledAccess("Message", XDC_EXT_TOKEN_value));

			JComponent cpLeft = XbrlDockUtilsGui.createSplit(false, pnlReport, gridMsg.getComp(), 0.8);
			cp.add(XbrlDockUtilsGui.createSplit(true, cpLeft, cpRight, 0.8), BorderLayout.CENTER);

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
						report.remove(id);
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

						report.put(id, val);
					}
				}
			} catch (Throwable e1) {
				XbrlDockException.wrap(e1);
			}

		}
	};

	ActionListener al = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			JComponent comp = (JComponent) e.getSource();

			if (comp instanceof AbstractButton) {
				AbstractButton abtn = (AbstractButton) comp;
				if (abtn.isSelected()) {
					report.put((String) abtn.getClientProperty(XDC_EXT_TOKEN_id), abtn.getActionCommand());
				}
			}
		}
	};

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
			
			if ( null != label ) {
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
		Collection<Map> atts = XbrlDockUtils.simpleGet(attDef, VSME_attributes);
		
		if ( null == atts ) {
			String src = XbrlDockUtils.simpleGet(attDef, XDC_GEN_TOKEN_source);
			atts = XbrlDockUtils.simpleGet(meta, "tables", src, VSME_attributes);
		}
		
		DefaultTableModel tm = new DefaultTableModel(4, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return true;
			}
		};

		for ( Map a : atts ) {
			tm.addColumn(a.get(XDC_EXT_TOKEN_id));
		}
		
		JTable tbl = new JTable(tm);
		tbl.setGridColor(Color.darkGray);
    JTableHeader header = tbl.getTableHeader();
    header.setBackground(Color.LIGHT_GRAY);
    header.setForeground(Color.blue);
    
		JScrollPane scp = new JScrollPane(tbl);
		scp.setPreferredSize(new Dimension(600,100));
		
		XbrlDockUtilsGui.setTitle(scp, i);
		
		tbl.addFocusListener(fa);
		
		return scp;
	}

	public JComponent createRadio(String i, Map<String, Object> attDef) {
		Collection<Object> options = XbrlDockUtils.simpleGet(attDef, VSME_options);

		if (null != options) {
			JPanel ret = new JPanel(new GridLayout(options.size(), 1));

			ButtonGroup bgr = new ButtonGroup();

			for (Object o : options) {
				String id = XbrlDockUtils.simpleGet(o, XDC_EXT_TOKEN_id);
				JRadioButton bt = new JRadioButton(id);
				bt.putClientProperty(XDC_EXT_TOKEN_id, i);
				bt.setActionCommand(id);
				bt.addActionListener(al);
				bgr.add(bt);
				ret.add(bt);
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
		return tf;
	}

}
