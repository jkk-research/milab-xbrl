package com.xbrldock.poc.gui;

import java.util.Map;

import javax.swing.JFrame;

import com.xbrldock.XbrlDock;
import com.xbrldock.poc.XbrlDockPoc;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockGuiApp extends JFrame implements XbrlDockGuiConsts {
	private static final long serialVersionUID = 1L;

	XbrlDockPoc xbrlDock;

	public XbrlDockGuiApp(XbrlDockPoc xbrlDock) throws Exception {
		super("XBRLDock PoC");
		
		this.xbrlDock = xbrlDock;

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void main(String[] args) {
		try {
			XbrlDockPoc xbrlDock = new XbrlDockPoc();

			xbrlDock.initEnv(args);

			if (xbrlDock.test()) {

				XbrlDockGuiApp frame = new XbrlDockGuiApp(xbrlDock);

				frame.setVisible(true);

				frame.pack();

				Map cfg = XbrlDock.getSubConfig(null, ConfigKey.gui, ConfigKey.frame);

				frame.setLocation((int) XbrlDock.getConfig(cfg, 10, ConfigKey.location, ConfigKey.x),
						(int) XbrlDock.getConfig(cfg, 10, ConfigKey.location, ConfigKey.y));
				frame.setSize((int) XbrlDock.getConfig(cfg, 200, ConfigKey.dimension, ConfigKey.x),
						(int) XbrlDock.getConfig(cfg, 100, ConfigKey.dimension, ConfigKey.y));
			}
		} catch (Throwable t) {
			XbrlDock.log(EventLevel.Exception, t);
		}
	}

}