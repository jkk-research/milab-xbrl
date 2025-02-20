package org.xbrldock.vsme.poc;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.xbrldock.XbrlDockConsts;
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
	GridLayout glReportPage;

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

			glReportPage = new GridLayout(1, 2);
			pnlReportPage = new JPanel(glReportPage);

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
		NodeList nl = standard.getElementsByTagName("*");
		int nc = nl.getLength();

		standardElements = new TreeMap<>();
		for (int idx = 0; idx < nc; ++idx) {
			Element e = (Element) nl.item(idx);
			String id = e.getAttribute(XDC_EXT_TOKEN_id);

			String att = e.getAttribute(XDC_EXT_TOKEN_class);

			if ((null != att) && att.contains("vmes_page")) {
				cbPageSelector.addItem(e.getTextContent());
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
					help = se.getTextContent();
				}
			}

			htmlHelp.setText(help);
		}
	};

	DocumentListener dl = new DocumentListener() {
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
					report.put(id, s);
				}
			} catch (Throwable e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
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

		glReportPage.setRows(items.size());

		for (String i : items) {
			JLabel label = new JLabel(i);
			pnlReportPage.add(label);
			Element se = standardElements.get(i);

			if (null != se) {
				label.setToolTipText(se.getTextContent());
			}

			Map<String, Object> attDef = XbrlDockUtils.simpleGet(meta, VSME_attributes, i);
			String attType = XbrlDockUtils.simpleGet(attDef, XDC_EXT_TOKEN_type);

			JComponent comp = null;
			switch (attType) {
			case "String":
			case "Real":
				comp = createTextField(i, attDef);
				break;
			case "Identifier":
				comp = createRadio(i, attDef);
				break;
			}

			pnlReportPage.add((null == comp) ? new JLabel(attType) : comp);
		}

		pnlReportPage.invalidate();
		pnlReportPage.getParent().revalidate();
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

	public JComponent createTextField(String i, Map<String, Object> attDef) {
		JTextField tf = new JTextField();

		tf.putClientProperty(XDC_EXT_TOKEN_id, i);
		tf.addFocusListener(fa);

		javax.swing.text.Document doc = tf.getDocument();
		doc.addDocumentListener(dl);
		doc.putProperty(XDC_EXT_TOKEN_id, i);
		return tf;
	}

}
