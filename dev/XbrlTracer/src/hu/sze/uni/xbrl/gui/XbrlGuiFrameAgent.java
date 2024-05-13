package hu.sze.uni.xbrl.gui;

import java.awt.BorderLayout;
import java.net.URL;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustAgent;

public class XbrlGuiFrameAgent extends DustAgent implements XbrlGuiConsts {
	
	public static void main(String[] args) {
		new Gui();
	}
	
	static class Gui {
		MindHandle pool;
		
		JFrame frame;
		JTabbedPane tpBottom;
		
//		JComboBox cbTaxonomySelector;
		JTextField tfConceptFilter;
		
		Gui() {			
			frame = new JFrame("XBRLDock - Plain Report Viewer 0.1");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			
			
			JTable tblFactGrid = new JTable();
			JSplitPane splTop = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tblFactGrid), tpBottom);
			
			tpBottom = new JTabbedPane();
			
			JSplitPane splMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splTop, tpBottom);
			JPanel pnlMain = new JPanel(new BorderLayout());
			pnlMain.add(splMain, BorderLayout.CENTER);
			frame.getContentPane().add(pnlMain, BorderLayout.CENTER);

			frame.pack();

			frame.setVisible(true);
			
			URL img = getClass().getResource("XBRLDock.jpg");
			ImageIcon i = new ImageIcon(img);
			frame.setIconImage(i.getImage());
			
			frame.setBounds(10, 10, 1000, 800);
		}
		
		void updatePool() {
			this.pool = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, MISC_ATT_CONN_SOURCE);
			
			Dust.log(EVENT_TAG_TYPE_INFO, "*** Taxonomies ***");

			Map<Object, Object> root;
			root = Dust.access(MindAccess.Peek, null, pool, XBRLDOCK_ATT_POOL_TAXONOMIES);
			for (Map.Entry<Object, Object> entry : root.entrySet()) {
				Map<Object, Object> children = Dust.access(MindAccess.Peek, null, entry.getValue(), XBRLDOCK_ATT_TAXONOMY_CONCEPTS);
				Dust.log(EVENT_TAG_TYPE_INFO, entry.getKey(), children.size());
			}

			Dust.log(EVENT_TAG_TYPE_INFO, "*** Reports ***");

			root = Dust.access(MindAccess.Peek, null, pool, XBRLDOCK_ATT_POOL_REPORTS);
			for (Map.Entry<Object, Object> entry : root.entrySet()) {
				Map<Object, Object> children = Dust.access(MindAccess.Peek, null, entry.getValue(), MISC_ATT_CONN_MEMBERMAP);
				Dust.log(EVENT_TAG_TYPE_INFO, entry.getKey(), children.size());
			}

		}
	}

	@Override
	protected MindHandle agentProcess() throws Exception {
		Gui gui = Dust.access(MindAccess.Peek, null, MIND_TAG_CONTEXT_SELF, DUST_ATT_IMPL_DATA);
		
		if ( null == gui ) {
			gui = new Gui();
			Dust.access(MindAccess.Set, gui, MIND_TAG_CONTEXT_SELF, DUST_ATT_IMPL_DATA);
		}

		gui.updatePool();

		return super.agentEnd();
	}

}
