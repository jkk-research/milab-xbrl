package com.xbrldock.poc.gui;

import java.awt.Component;
import java.util.Map;

import javax.swing.JComponent;

import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings("rawtypes")
public class XbrlDockGuiUtils extends XbrlDockUtilsGui implements XbrlDockGuiConsts {

	public static void placementLoad(Component comp, Map config) {
		Object c;

		c = XbrlDockUtils.simpleGet(config, XDC_CFG_GEOM_location);
		if (c instanceof Map) {
			comp.setLocation(XbrlDockUtils.getInt(c, XDC_CFG_GEOM_x), XbrlDockUtils.getInt(c, XDC_CFG_GEOM_y));
		}

		c = XbrlDockUtils.simpleGet(config, XDC_CFG_GEOM_dimension);
		if (c instanceof Map) {
			comp.setSize(XbrlDockUtils.getInt(c, XDC_CFG_GEOM_x), XbrlDockUtils.getInt(c, XDC_CFG_GEOM_y));
		}
	}

	public static JComponent setTitle(ComponentWrapper cw, String title) {
		return setTitle(cw.getComp(), title);
	}

}
