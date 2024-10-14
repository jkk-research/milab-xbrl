package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts.GenAgent;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "rawtypes", /* "unchecked" */ })
public class XbrlDockGuiMetaManagerPanel extends JPanel implements XbrlDockGuiConsts, GenAgent {
	private static final long serialVersionUID = 1L;

	ArrayList<Object> items = new ArrayList<>();
	Object selItem;

	XbrlDockGuiUtilsGridModel mdlGrid = new XbrlDockGuiUtilsGridModel(items, 
			new LabeledAccess("Identifier", "", XDC_METAINFO_pkgInfo, XDC_EXT_TOKEN_identifier),
			new LabeledAccess("Requires", FMT_COLL, XDC_GEN_TOKEN_requires), 
			new LabeledAccess("File count", FMT_COLL, XDC_METATOKEN_includes),
			new LabeledAccess("Entry points", FMT_COLL, XDC_METAINFO_entryPoints), 
			new LabeledAccess("Items", 0L, XDC_METATOKEN_items),
			new LabeledAccess("Links", 0L, XDC_METATOKEN_links), 
			new LabeledAccess("References", 0L, XDC_METATOKEN_references)
	);
	
	JTable tblGrid;

	JButton dropZone;

	JEditorPane txtInfo;
	String placeholder;

	ActionListener al = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {

			}
		}
	};

	public XbrlDockGuiMetaManagerPanel() throws Exception {
		super(new BorderLayout());

		tblGrid = new JTable(mdlGrid);

		ListSelectionModel lm = tblGrid.getSelectionModel();
		lm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lm.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int sr = tblGrid.getSelectedRow();
					selItem = (-1 == sr) ? null : items.get(sr);
					updateDescPanel();
				}
			}
		});

		tblGrid.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (1 < e.getClickCount()) {
					String selId = XbrlDockUtils.simpleGet(selItem, XDC_METAINFO_pkgInfo, XDC_EXT_TOKEN_identifier);
					XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_gui, XDC_CMD_GEN_SELECT, XDC_CFGTOKEN_AGENT_metaManager, selId);
				}
			}
		});

		txtInfo = new JEditorPane();
		txtInfo.setContentType("text/html");
		txtInfo.setEditable(false);

		dropZone = new JButton("<html><body>Taxonomy<br>drop zone</body></html>");
		dropZone.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		dropZone.setMaximumSize(new Dimension(50, 500));

		JPanel pnlTop = new JPanel(new BorderLayout());

		pnlTop.add(new JScrollPane(tblGrid), BorderLayout.CENTER);
		pnlTop.add(dropZone, BorderLayout.EAST);
		add(XbrlDockUtilsGui.createSplit(false, pnlTop, new JScrollPane(txtInfo), 0.2), BorderLayout.CENTER);
	}

	protected void updateDescPanel() {
		String txt = (null == selItem) ? placeholder : FMT_TOHTML.toString(selItem);

		int cp = txtInfo.getCaretPosition();

		txtInfo.setText("<html><body>" + txt + "</body></html>");

		txtInfo.setCaretPosition(cp);
	}

	@Override
	public void initModule(Map config) throws Exception {
		Map metaCatalog = XbrlDock.callAgent(XDC_CFGTOKEN_AGENT_metaManager, XDC_CMD_GEN_GETCATALOG);
		placeholder = XbrlDockUtils.simpleGet(config, XDC_GEN_TOKEN_placeholder);

		items.clear();
		for (Object c : metaCatalog.values()) {
			items.add(c);
		}

		mdlGrid.fireTableDataChanged();

		updateDescPanel();

	}

	@Override
	public Object process(String command, Object... params) {
		// TODO Auto-generated method stub
		return null;
	}

}
