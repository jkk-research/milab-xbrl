package com.xbrldock.utils;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder;

import com.xbrldock.XbrlDockException;

public class XbrlDockUtilsGui implements XbrlDockUtilsConsts {

	public interface WidgetContainer {
		JComponent getWidget();

		void setGuiValue(Object value);
	}

	public static JComponent setTitle(JComponent comp, String title) {
		comp.setBorder(new TitledBorder(title));
		return comp;
	}

	public static void nextGBRow(GridBagConstraints gbc, boolean reset) {
		++gbc.gridy;
		gbc.gridx = -1;
		gbc.gridwidth = gbc.gridheight = 1;
		nextGBCell(gbc, reset);
	}

	public static void nextGBCell(GridBagConstraints gbc, boolean reset) {
		gbc.gridx += gbc.gridwidth;

		if (reset) {
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
//		setTitle(cb, XbrlDockUtils.camel2Label(cmd));
		return cb;
	}

	public static AbstractButton setActive(AbstractButton ab, String cmd, ActionListener al) {
		ab.addActionListener(al);
		ab.setActionCommand(cmd);
		ab.setText(XbrlDockUtils.camel2Label(cmd));
		return ab;
	}

	public static Icon getIcon(String id) {
		return getIcon(id, null);
	}

	public static Icon getIcon(String id, Dimension prefSize) {
		Image img = Toolkit.getDefaultToolkit().getImage("res/" + id + ".png");

		if (null != prefSize) {
			img = img.getScaledInstance(prefSize.width, prefSize.width, java.awt.Image.SCALE_SMOOTH);
		}

		return new ImageIcon(img);
	}

}
