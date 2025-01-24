package com.xbrldock.poc.gui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.xbrldock.poc.meta.XbrlDockMetaContainer;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockGuiMetaItemPanel extends JPanel implements XbrlDockGuiConsts {
	private static final long serialVersionUID = 1L;

	XbrlDockMetaContainer taxonomy;

	ArrayList<String> attNames = new ArrayList<>();

	Map selItem;

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

	ItemModel mdlItem;
	JTable tblItem;
	
	XbrlDockGuiUtilsHtmlDisplay itemTexts = new XbrlDockGuiUtilsHtmlDisplay();
	XbrlDockGuiUtilsHtmlDisplay itemRefs = new XbrlDockGuiUtilsHtmlDisplay();

	JTabbedPane tpItem;

	public XbrlDockGuiMetaItemPanel() {
		super(new BorderLayout());

		tpItem = new JTabbedPane();

		mdlItem = new ItemModel();
		tblItem = new JTable(mdlItem);

		tpItem.addTab("Properties", new JScrollPane(tblItem));

		tpItem.addTab("Texts", itemTexts.getComp());
		tpItem.addTab("References", itemRefs.getComp());

		add(tpItem, BorderLayout.CENTER);
	}

	public void setTaxonomy(XbrlDockMetaContainer taxonomy) throws Exception {

		this.taxonomy = taxonomy;

		Set<String> s = new TreeSet<>();

		taxonomy.visit(XDC_METATOKEN_items, new GenAgent() {
			
			@Override
			public Object process(String cmd, Map params) throws Exception {
				switch (cmd) {
				case XDC_CMD_GEN_Process:
					Map.Entry<String, Map> l = (Map.Entry<String, Map> ) params.get(XDC_EXT_TOKEN_value);
					s.addAll(l.getValue().keySet());
					break;
				default:
					break;
				}
				return true;
			}
		});

		attNames.clear();
		attNames.addAll(s);
	}

	public void setSelItem(Map selItem) {
		if (this.selItem != selItem) {
			this.selItem = selItem;
			if (null != selItem) {
				mdlItem.fireTableDataChanged();

				String itemId = (String) selItem.get(XDC_METATOKEN_url);

				StringBuilder sbText = new StringBuilder();

				taxonomy.visit(XDC_METATOKEN_labels, new GenAgent() {
					
					@Override
					public Object process(String cmd, Map params) throws Exception {
						switch (cmd) {
						case XDC_CMD_GEN_Process:
							Map.Entry<String, Object> le = (Map.Entry<String, Object>) params.get(XDC_EXT_TOKEN_value);
							sbText.append("\t<tr><td>").append(le.getKey()).append("</td><td>").append(le.getValue()).append("</td></tr>\n");
							break;
						default:
							break;
						}
						return true;
					}
				}, itemId, "en");

				if (0 < sbText.length()) {
					sbText.insert(0, "<html><body><table>\n").append("</table></body></html>");
					tpItem.setEnabledAt(1, true);
				} else {
					tpItem.setEnabledAt(1, false);
				}

				itemTexts.setText(sbText.toString());

//				Map<String, Object> labels = taxonomy.getItemLabels(itemId, "en");
//				if (null != labels) {
//					sbText = new StringBuilder("<html><body><table>\n");
//					for (Map.Entry<String, Object> le : labels.entrySet()) {
//						sbText.append("\t<tr><td>").append(le.getKey()).append("</td><td>").append(le.getValue()).append("</td></tr>\n");
//					}
//					sbText.append("</table></body></html>");
//
//					txtItem.setText(sbText.toString());
//				}

				tpItem.setEnabledAt(1, 0 < sbText.length());

				StringBuilder sbRefs = new StringBuilder();
				itemRefs.setText("");

				taxonomy.visit(XDC_METATOKEN_refLinks, new GenAgent() {
					boolean cont = false;

					@Override
					public Object process(String cmd, Map params) throws Exception {
						switch (cmd) {
						case XDC_CMD_GEN_Begin:
							sbRefs.append("<html><body><table>\n");
							break;
						case XDC_CMD_GEN_Process:

							if (cont) {
								sbRefs.append("\t<tr colspan=2><td> --- </td></tr>\n");
							} else {
								cont = true;
							}
							
							Map<String, Object> re = ( Map<String, Object>) params.get(XDC_EXT_TOKEN_value);
							for (Map.Entry<String, Object> le : re.entrySet()) {
								String k = le.getKey();
								Object v = le.getValue();

								if (k.endsWith(":URI")) {
									v = "<a href=\"" + v + "\">" + v + "</a>";
								}
								sbRefs.append("\t<tr><td>").append(k).append("</td><td>").append(v).append("</td></tr>\n");
							}
							break;
						case XDC_CMD_GEN_End:
							sbRefs.append("</table></body></html>");
							itemRefs.setText(sbRefs.toString());
							break;
						default:
							break;
						}
						return true;
					}
				});

				tpItem.setEnabledAt(2, 0 < sbRefs.length());

//				if ( !sbRefs.isEmpty() ) {
//				Iterable<Map<String, Object>> refs = taxonomy.getItemRefs(itemId);
//				if (null != refs) {
//					for (Map<String, Object> re : refs) {
//						if (null == sbRefs) {
//							sbRefs = new StringBuilder("<html><body><table>\n");
//						} else {
//							sbRefs.append("\t<tr colspan=2><td> --- </td></tr>\n");
//						}
//						for (Map.Entry<String, Object> le : re.entrySet()) {
//							String k = le.getKey();
//							Object v = le.getValue();
//
//							if (k.endsWith(":URI")) {
//								v = "<a href=\"" + v + "\">" + v + "</a>";
//							}
//							sbRefs.append("\t<tr><td>").append(k).append("</td><td>").append(v).append("</td></tr>\n");
//						}
//					}
//					sbRefs.append("</table></body></html>");

//					txtRefs.setText(sbRefs.toString());
//					tpItem.setEnabledAt(2, true);
//				} else {
//					tpItem.setEnabledAt(2, false);
//				}
			}

//			tpItem.setEnabledAt(2, (null != sbRefs));

			int si = tpItem.getSelectedIndex();
			if (!tpItem.isEnabledAt(si)) {
				tpItem.setSelectedIndex(0);
			}
		}
	}
}
