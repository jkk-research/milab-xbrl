package com.xbrldock.poc.gui;

import java.awt.Desktop;
import java.net.URL;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.xbrldock.XbrlDockException;
import com.xbrldock.utils.XbrlDockUtils;

public class XbrlDockGuiUtilsHtmlDisplay implements XbrlDockGuiConsts.ComponentWrapper<JComponent>, XbrlDockGuiConsts {
	
	String placeholder;
	JEditorPane txtInfo;
	JComponent comp;

	public XbrlDockGuiUtilsHtmlDisplay() {
		placeholder = "";
		txtInfo = new JEditorPane();
		txtInfo.setContentType("text/html");
		txtInfo.setEditable(false);
		
		txtInfo.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent event) {
				if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					URL url = event.getURL();
					try {
						Desktop.getDesktop().browse(url.toURI());
					} catch (Throwable ex) {
						XbrlDockException.swallow(ex, "Failed to click on url", url);
					}
				}
			}
		});
		
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
		
		if ( cp > txt.length() ) {
			cp = txt.length();
		}

		txtInfo.setText("<html><body>" + content + "</body></html>");

		txtInfo.setCaretPosition(cp);
	}
	
	@Override
	public JComponent getComp() {
		return comp;
	}

}