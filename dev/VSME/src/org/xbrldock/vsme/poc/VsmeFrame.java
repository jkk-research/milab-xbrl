package org.xbrldock.vsme.poc;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileNameExtensionFilter;

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
import com.xbrldock.utils.XbrlDockUtilsGui.WidgetContainer;
import com.xbrldock.utils.XbrlDockUtilsHtml;
import com.xbrldock.utils.XbrlDockUtilsJson;
import com.xbrldock.utils.XbrlDockUtilsMvel;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class VsmeFrame implements VsmePocConsts, XbrlDockConsts.GenAgent, VsmePocConsts.EditContext {
	Document standard;
	Map<String, Object> meta;

	Map<String, Object> panelMap;
	Map<String, Element> standardElements;

	Map<String, Object> report;
	Set<String> protAtts = new TreeSet<>();
	ArrayList<Map<String, String>> messages;

	JFrame frame;
	XbrlDockGuiUtilsHtmlDisplay htmlStandard;
	XbrlDockGuiUtilsHtmlDisplay htmlHelp;
	Map<String, WidgetContainer> editors = new TreeMap<>();

	ArrayList<String> pageIDs = new ArrayList<>();
	JComboBox<String> cbPageSelector;
	JPanel pnlValueEditor;
	JPanel pnlReportPage;

	XbrlDockGuiWidgetGrid gridMsg;

	JFileChooser fc;
	Icon iProt;

	@Override
	public Object process(String cmd, Map<String, Object> params) throws Exception {
		switch (cmd) {
		case XDC_CMD_GEN_Init:
			String fName = XbrlDockUtils.simpleGet(params, VSME_meta);
			meta = XbrlDockUtilsJson.readJson(new File(fName));

			fName = XbrlDockUtils.simpleGet(meta, VSME_meta, VSME_standard);
			standard = XbrlDockUtilsHtml.readHtml(fName);

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

			Dimension dimBtn = new Dimension(15, 15);
			iProt = XbrlDockUtilsGui.getIcon(VSME_protect, dimBtn);

			break;
		case XDC_CMD_GEN_Begin:

			readStandard();

			panelMap = new TreeMap<String, Object>();
			Collection<Map<String, Object>> panels = XbrlDockUtils.simpleGet(meta, VSME_panels);
			for (Map<String, Object> p : panels) {
				String id = XbrlDockUtils.simpleGet(p, XDC_EXT_TOKEN_id);
				panelMap.put(id, p);
			}

			report = new TreeMap<String, Object>();

			setReportPage(XbrlDockUtils.simpleGet(meta, XDC_GEN_TOKEN_start));

			cbPageSelector.addActionListener(al);

			frame.pack();

			frame.setVisible(true);

			break;
		}
		return null;
	}

	@Override
	public Map<String, Object> getMeta() {
		return meta;
	}

	@Override
	public Map<String, Object> getReport() {
		return report;
	}

	@Override
	public Document getStandard() {
		return standard;
	}

	void readStandard() {
		Elements nl = standard.getAllElements();

		standardElements = new TreeMap<>();

		for (Element e : nl) {
			String id = e.attr(XDC_EXT_TOKEN_id);

			String att = e.attr(XDC_EXT_TOKEN_class);

			if ((null != att) && att.contains("vmes_page")) {
				cbPageSelector.addItem(e.text());
				pageIDs.add(id);
			}

			if (!XbrlDockUtils.isEmpty(id)) {
				standardElements.put(id, e);
			}
		}
	}

	@Override
	public void activateEditor(Object e, Map<String, Object> p) {
		JLabel lb = (JLabel) pnlValueEditor.getComponent(0);

		lb.setText(XbrlDockUtils.sbAppend(null, ": ", false, e, p).toString());
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
						if (!protAtts.isEmpty()) {
							report.put(VSME_protect, new ArrayList(protAtts));
						}
						XbrlDockUtilsJson.writeJson(file, report);
						if (!protAtts.isEmpty()) {
							report.put(VSME_protect, protAtts);
						}
					}
					break;
				case XDC_CMD_GEN_LOAD:
					fcRet = fc.showOpenDialog(frame);

					if (fcRet == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						report = XbrlDockUtilsJson.readJson(file);

						Collection<String> c = (Collection) report.get(VSME_protect);
						if (null != c) {
							report.remove(VSME_protect);
							for (String p : c) {
								setGuiProtected(p, true);
							}
						}

						for (String k : new TreeSet<String>(report.keySet())) {
							Object val = report.get(k);
//							XbrlDock.log(EventLevel.Trace, k, val);
							setGuiValue(val, k);
						}
					}
					break;
				case VSME_protect:
					JToggleButton tb = (JToggleButton) e.getSource();
					String id = (String) tb.getClientProperty(XDC_EXT_TOKEN_id);

					boolean prot = tb.isSelected();

					setGuiProtected(id, prot);
					setGuiValue(protAtts, cmd);
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

			if (comp == cbPageSelector) {
				int pidx = cbPageSelector.getSelectedIndex();

				setReportPage(pageIDs.get(pidx));
			}

			if (comp instanceof AbstractButton) {
				AbstractButton abtn = (AbstractButton) comp;
				if (abtn.isSelected()) {
					setReportValue(abtn.getActionCommand(), (String) abtn.getClientProperty(XDC_EXT_TOKEN_id));
				}
			}
		}
	};

	protected void setGuiProtected(String id, boolean prot) {
		if (prot) {
			protAtts.add(id);
			report.put(VSME_protect, protAtts);
		} else {
			protAtts.remove(id);
			if (protAtts.isEmpty()) {
				report.remove(VSME_protect);
			}
		}

		editors.get(id).getWidget().setEnabled(!prot);
	}

	private void setGuiValue(Object value, String attId) {
		WidgetContainer wc = editors.get(attId);

		if (null != wc) {
			wc.setGuiValue(value);
		}
	};

	@Override
	public Object setReportValue(Object value, String attId, int row, String col) {
		Map<String, Object> target = report;
		Object orig = target.get(attId);
		String key = attId;

		if (-1 == row) {
			if (null == value) {
				target.remove(attId);
			}
		} else {
			key = col;
			target = (Map<String, Object>) ((ArrayList) orig).get(row);
			orig = target.get(key);
		}

		if (!XbrlDockUtils.isEqual(value, orig)) {
			target.put(key, value);
			reportUpdated(value, attId, row, col);
		}

		return orig;
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

		if (null != items) {

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(2, 2, 2, 2);

			for (String i : items) {
				XbrlDockUtilsGui.nextGBRow(gbc, true);
				JLabel label = new JLabel(i);

				Map<String, Object> attDef = XbrlDockUtils.simpleGet(meta, VSME_attributes, i);
				String attType = XbrlDockUtils.simpleGet(attDef, XDC_EXT_TOKEN_type);

				WidgetContainer wc = null;
				switch (attType) {
				case "Table":
					wc = new VsmeDataGrid(this, i, attDef);
					label = null;
					break;
				default:
					wc = new VsmeDataEditor(this, i, attDef);
					break;
				}

				editors.put(i, wc);
				JComponent comp = wc.getWidget();

				gbc.anchor = GridBagConstraints.PAGE_START;
				JToggleButton btProtect = XbrlDockUtilsGui.createBtn(VSME_protect, alCmd, JToggleButton.class);
				btProtect.setText("");
				btProtect.setIcon(iProt);

				pnlReportPage.add(btProtect, gbc);
				btProtect.putClientProperty(XDC_EXT_TOKEN_id, i);
				XbrlDockUtilsGui.nextGBCell(gbc, true);

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
				} else {
					comp.putClientProperty(XDC_EXT_TOKEN_id, i);
					comp.addFocusListener(fa);
				}

				gbc.weightx = 1.0;

				pnlReportPage.add(comp, gbc);

				Object val = report.get(i);
				if (null != val) {
					setGuiValue(val, i);
				}
			}
		}

		pnlReportPage.invalidate();
		pnlReportPage.getParent().revalidate();
	}

}
