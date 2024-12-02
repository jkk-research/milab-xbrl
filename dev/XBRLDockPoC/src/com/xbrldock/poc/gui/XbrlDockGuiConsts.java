package com.xbrldock.poc.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;

import com.xbrldock.poc.XbrlDockPocConsts;

public interface XbrlDockGuiConsts extends XbrlDockPocConsts {	
	
	String XDC_GUI_COMMANDS = "guiCommands";
	
	String XDC_GUICMD_PICK = "xdc_guiPick";
	String XDC_GUICMD_ACTIVATE = "xdc_guiActivate";
	String XDC_GUICMD_SELCHG = "xdc_guiSelChg";
	
	String XDC_GRIDCOL_ROWNUM = "xdc_gridcolRowNum";
	String XDC_GRIDCOL_SELECTED = "xdc_gridcolSelected";
	
	class WidgetEvent extends ActionEvent {
		private static final long serialVersionUID = 1L;
		
		private static final Object[] NO_HINTS = {};
		
		private final Object userOb;
		private final Object[] hints;
		
		public WidgetEvent(Object source, String command) {
			super(source, 0, command);
			
			this.userOb = null;
			this.hints = NO_HINTS;
		}
		
		public WidgetEvent(Object source, String command, int id, int modifiers) {
			super(source, 0, command, modifiers);
			
			this.userOb = null;
			this.hints = NO_HINTS;
		}
		
		public WidgetEvent(Object source, String command, Object userOb, Object... hints ) {
			super(source, 0, command);
			
			this.userOb = userOb;
			this.hints = hints;
		}
		
		public Object getUserOb() {
			return userOb;
		}
		
		public Object[] getHints() {
			return hints;
		}
	}
	
	interface ComponentWrapper<CompType extends JComponent> {
		CompType getComp();
	}
	
	interface Widget<CompType extends JComponent> extends ComponentWrapper<CompType>{
		void setActionListener(ActionListener al, String... guiCmds);
	}
}
