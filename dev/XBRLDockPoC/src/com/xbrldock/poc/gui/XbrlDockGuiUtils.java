package com.xbrldock.poc.gui;

import java.awt.Component;
import java.util.Map;

import com.xbrldock.utils.XbrlDockUtils;

@SuppressWarnings("rawtypes")
public class XbrlDockGuiUtils implements XbrlDockGuiConsts {

	public static void placementLoad(Component comp, Map config) {
		Object c;
		
		c = XbrlDockUtils.simpleGet(config, XDC_CFG_GEOM_location);
		if ( c instanceof Map ) {
			comp.setLocation(XbrlDockUtils.getInt(c, XDC_CFG_GEOM_x), XbrlDockUtils.getInt(c, XDC_CFG_GEOM_y));
		}
		
		c = XbrlDockUtils.simpleGet(config, XDC_CFG_GEOM_dimension);
		if ( c instanceof Map ) {
			comp.setSize(XbrlDockUtils.getInt(c, XDC_CFG_GEOM_x), XbrlDockUtils.getInt(c, XDC_CFG_GEOM_y));
		}
	}
}
