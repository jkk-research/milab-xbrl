package com.xbrldock.poc.gui;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import com.xbrldock.utils.XbrlDockUtils;

public class XbrlDockGuiUtilsHtmlDisplay extends JEditorPane implements XbrlDockGuiConsts.ComponentWrapper<JComponent>, XbrlDockGuiConsts {
	private static final long serialVersionUID = 1L;
	
	String placeholder;
	JEditorPane txtInfo;
	JComponent comp;

	public XbrlDockGuiUtilsHtmlDisplay() {
		placeholder = "";
		txtInfo = new JEditorPane();
		txtInfo.setContentType("text/html");
		txtInfo.setEditable(false);
		
		comp = new JScrollPane(txtInfo);
	}
	
	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}
	
	public void setObject(Object ob) {		
		setText(FMT_TOHTML.toString(ob, null));
	}
	
	public void setText(String txt) {
		String content = XbrlDockUtils.isEmpty(txt) ? placeholder : txt;

		int cp = txtInfo.getCaretPosition();

		txtInfo.setText("<html><body>" + content + "</body></html>");

		txtInfo.setCaretPosition(cp);
	}
	
	@Override
	public JComponent getComp() {
		return comp;
	}

}