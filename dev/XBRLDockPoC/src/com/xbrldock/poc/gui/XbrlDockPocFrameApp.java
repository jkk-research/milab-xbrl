package com.xbrldock.poc.gui;

import java.util.Map;

import javax.swing.JFrame;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts.XbrlEventLevel;
import com.xbrldock.XbrlDockConsts.XbrlGeometry;
import com.xbrldock.poc.XbrlDockPoc;
import com.xbrldock.poc.XbrlDockPocConsts.XbrlDockPocApp;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockPocFrameApp extends JFrame {
	private static final long serialVersionUID = 1L;

	XbrlDockPoc xbrlDock;

	public XbrlDockPocFrameApp(String[] args) throws Exception {
		super("XBRLDock PoC");

		this.xbrlDock = new XbrlDockPoc();

		xbrlDock.initEnv(args);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void main(String[] args) {
		try {
			XbrlDockPocFrameApp frame = new XbrlDockPocFrameApp(args);

			frame.setVisible(true);
			
			frame.pack();
			
			Map cfg = XbrlDock.getSubConfig(null, XbrlDockPocApp.gui, XbrlDockPocApp.frame);
			
			frame.setLocation((int) XbrlDock.getConfig(cfg, 10, XbrlGeometry.location, XbrlGeometry.x), (int) XbrlDock.getConfig(cfg, 10, XbrlGeometry.location, XbrlGeometry.y));
			frame.setSize((int) XbrlDock.getConfig(cfg, 200, XbrlGeometry.dimension, XbrlGeometry.x), (int) XbrlDock.getConfig(cfg, 100, XbrlGeometry.dimension, XbrlGeometry.y));

		} catch (Throwable t) {
			XbrlDock.log(XbrlEventLevel.Exception, t);
		}
	}

}
