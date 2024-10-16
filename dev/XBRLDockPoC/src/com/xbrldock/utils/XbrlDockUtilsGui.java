package com.xbrldock.utils;

import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder;

import com.xbrldock.XbrlDockException;

public class XbrlDockUtilsGui implements XbrlDockUtilsConsts {
	
	public static JSplitPane createSplit(boolean horizontal, JComponent c1, JComponent c2, double weight) {
		JSplitPane spp = new JSplitPane(horizontal ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT, c1, c2);
		spp.setResizeWeight(weight);
		spp.setContinuousLayout(true);
		return spp;
	}


	public static JComponent setTitle(JComponent comp, String title) {
		comp.setBorder(new TitledBorder(title));
		return comp;
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

	public static void setActive(AbstractButton ab, String cmd, ActionListener al) {
		ab.addActionListener(al);
		ab.setActionCommand(cmd);
		ab.setText(XbrlDockUtils.camel2Label(cmd));
	}
}
