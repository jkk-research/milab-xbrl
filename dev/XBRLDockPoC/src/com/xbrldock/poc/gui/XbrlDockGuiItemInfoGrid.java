package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.XbrlDockPoc;
import com.xbrldock.poc.taxonomy.XbrlDockTaxonomy;
import com.xbrldock.utils.XbrlDockUtilsGui;

//@SuppressWarnings({ "rawtypes", "unchecked" })
@SuppressWarnings({ "rawtypes" })
public class XbrlDockGuiItemInfoGrid extends JPanel implements XbrlDockGuiConsts {
	private static final long serialVersionUID = 1L;

	XbrlDockPoc xbrlDock;
	XbrlDockTaxonomy taxonomy;

	ArrayList<String> attNames = new ArrayList<>();

	ArrayList<Map> items = new ArrayList<>();
	Map selItem;

	class GridModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		@Override
		public int getRowCount() {
			return items.size();
		}

		@Override
		public int getColumnCount() {
			return attNames.size();
		}

		@Override
		public String getColumnName(int column) {
			return attNames.get(column);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			String k = attNames.get(columnIndex);
			return items.get(rowIndex).get(k);
		}
	}

	class ItemModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		@Override
		public int getRowCount() {
			return attNames.size();
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int column) {
			return (0 == column) ? "Attribute" : "Value";
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			String an = attNames.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return an;
			case 1:
				return (null == selItem) ? "" : selItem.get(an);
			}
			return "";
		}
	}

	GridModel mdlGrid;
	JTable tblGrid;

	ItemModel mdlItem;
	JTable tblItem;

	JEditorPane txtItem;
	JEditorPane txtRefs;

	JTabbedPane tpItem;

	public XbrlDockGuiItemInfoGrid(XbrlDockPoc xbrlDock) {
		super(new BorderLayout());

		this.xbrlDock = xbrlDock;

		mdlGrid = new GridModel();
		tblGrid = new JTable(mdlGrid);

		ListSelectionModel lm = tblGrid.getSelectionModel();
		lm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lm.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int sr = tblGrid.getSelectedRow();
					StringBuilder sbText = null;
					StringBuilder sbRefs = null;

					if (-1 == sr) {
						selItem = null;
					} else {
						selItem = (-1 == sr) ? null : items.get(sr);
						mdlItem.fireTableDataChanged();

						String itemId = (String) selItem.get("id");

						Map<String, Object> labels = taxonomy.getItemLabels(itemId, "en");
						if (null != labels) {
							sbText = new StringBuilder("<html><body><table>\n");
							for (Map.Entry<String, Object> le : labels.entrySet()) {
								sbText.append("\t<tr><td>").append(le.getKey()).append("</td><td>").append(le.getValue()).append("</td></tr>\n");
							}
							sbText.append("</table></body></html>");

							txtItem.setText(sbText.toString());
						}

						Iterable<Map<String, Object>> refs = taxonomy.getItemRefs(itemId);
						if (null != refs) {
							for (Map<String, Object> re : refs) {
								if (null == sbRefs) {
									sbRefs = new StringBuilder("<html><body><table>\n");
								} else {
									sbRefs.append("\t<tr colspan=2><td> --- </td></tr>\n");
								}
								for (Map.Entry<String, Object> le : re.entrySet()) {
									String k = le.getKey();
									Object v = le.getValue();

									if (k.endsWith(":URI")) {
										v = "<a href=\"" + v + "\">" + v + "</a>";
									}
									sbRefs.append("\t<tr><td>").append(k).append("</td><td>").append(v).append("</td></tr>\n");
								}
							}
							sbRefs.append("</table></body></html>");

							txtRefs.setText(sbRefs.toString());

						}
					}

					tpItem.setEnabledAt(1, (null != sbText));
					tpItem.setEnabledAt(2, (null != sbRefs));

					int si = tpItem.getSelectedIndex();
					if (!tpItem.isEnabledAt(si)) {
						tpItem.setSelectedIndex(0);
					}
				}
			}
		});

		tpItem = new JTabbedPane();

		mdlItem = new ItemModel();
		tblItem = new JTable(mdlItem);

		tpItem.addTab("Properties", new JScrollPane(tblItem));

		txtItem = new JEditorPane();
		txtItem.setContentType("text/html");
		txtItem.setEditable(false);
		tpItem.addTab("Texts", new JScrollPane(txtItem));

		txtRefs = new JEditorPane();
		txtRefs.setContentType("text/html");
		txtRefs.setEditable(false);
		tpItem.addTab("References", new JScrollPane(txtRefs));

		txtRefs.addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent event) {
				if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					URL url = event.getURL();
					try {
						Desktop.getDesktop().browse(url.toURI());
					} catch (Throwable ex) {
						XbrlDockException.swallow(ex, "Failed to click on url", url);
					}
				}
			}
		});

		JSplitPane sp = XbrlDockUtilsGui.createSplit(false, new JScrollPane(tblGrid), tpItem, 0.5);

		add(sp, BorderLayout.CENTER);
	}

	public void setTaxonomy(XbrlDockTaxonomy taxonomy) throws Exception {

		this.taxonomy = taxonomy;

		Set<String> s = new TreeSet<>();
		for (String ii : taxonomy.getItemIds()) {
			for (String k : taxonomy.getItem(ii).keySet()) {
				s.add(k);
			}
		}

		attNames.clear();
		attNames.addAll(s);

		mdlGrid.fireTableStructureChanged();
	}

	public void displayItems(Collection<String> ids) {
		items.clear();

		for (String i : ids) {
			items.add(taxonomy.getItem(i));
		}

		mdlGrid.fireTableDataChanged();
	}

}
