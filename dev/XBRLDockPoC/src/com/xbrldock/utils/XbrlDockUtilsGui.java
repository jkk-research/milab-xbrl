package com.xbrldock.utils;

import javax.swing.JComponent;
import javax.swing.JSplitPane;

public class XbrlDockUtilsGui implements XbrlDockUtilsConsts {
	
	public static JSplitPane createSplit(boolean horizontal, JComponent c1, JComponent c2, double weight) {
		JSplitPane spp = new JSplitPane(horizontal ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT, c1, c2);
		spp.setResizeWeight(weight);
		spp.setContinuousLayout(true);
		return spp;
	}

}
