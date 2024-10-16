package com.xbrldock.poc.gui;

import javax.swing.JComponent;

import com.xbrldock.poc.XbrlDockPocConsts;

public interface XbrlDockGuiConsts extends XbrlDockPocConsts {
	String XDC_APP_SHOWITEMS = "Show items";
	String XDC_APP_SETROLETYPE = "Set role type";
	String XDC_APP_SETLANG = "Set language";
	String XDC_APP_SETENTRYPOINT = "Set entry point";
	
	interface ComponentWrapper<CompType extends JComponent> {
		CompType getComp();
	}
}
