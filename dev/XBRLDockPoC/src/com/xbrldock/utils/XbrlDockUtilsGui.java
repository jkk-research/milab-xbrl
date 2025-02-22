package com.xbrldock.utils;

import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder;

import com.xbrldock.XbrlDockException;

public class XbrlDockUtilsGui implements XbrlDockUtilsConsts {
	
	public static JComponent setTitle(JComponent comp, String title) {
		comp.setBorder(new TitledBorder(title));
		return comp;
	}

	public static void nextGBRow(GridBagConstraints gbc, boolean reset) {
		++ gbc.gridy;
		gbc.gridx = -1;
		nextGBCell(gbc, reset);
	}

	public static void nextGBCell(GridBagConstraints gbc, boolean reset) {
		++ gbc.gridx;
		
		if ( reset ) {
			gbc.gridwidth = gbc.gridheight = 1;
			gbc.ipadx = gbc.ipady = 0;
			gbc.weightx = gbc.weighty = 0.0;			
			gbc.fill = GridBagConstraints.HORIZONTAL;
//			gbc.insets = null;
			gbc.anchor = GridBagConstraints.CENTER;
		}
	}

	public static JSplitPane createSplit(boolean horizontal, JComponent c1, JComponent c2, double weight) {
		JSplitPane spp = new JSplitPane(horizontal ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT, c1, c2);
		spp.setResizeWeight(weight);
		spp.setContinuousLayout(true);
		return spp;
	}


	public static <BtClass extends AbstractButton> BtClass createBtn(String cmd, ActionListener al, Class<BtClass> bc) {
		try {
			BtClass b = bc.getConstructor().newInstance();
			setActive(b, cmd, al);
			return b;
		} catch (Throwable e) {
			return XbrlDockException.wrap(e, "Create btn for cmd", cmd, "class", bc);
		}
	}

	public static JComboBox<?> setActive(JComboBox<?> cb, String cmd, ActionListener al) {
		cb.addActionListener(al);
		cb.setActionCommand(cmd);
		setTitle(cb, XbrlDockUtils.camel2Label(cmd));
		return cb;
	}

	public static AbstractButton setActive(AbstractButton ab, String cmd, ActionListener al) {
		ab.addActionListener(al);
		ab.setActionCommand(cmd);
		ab.setText(XbrlDockUtils.camel2Label(cmd));
		return ab;
	}
}
