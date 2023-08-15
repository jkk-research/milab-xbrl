package hu.sze.uni.http;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts.MindAccess;

@SuppressWarnings("rawtypes") 
public class DustHttpServerJetty extends DustHttpServerBase {
	enum Commands {
		stop, info,
	}

	Server jetty;
	HandlerList handlers;
	ServletContextHandler ctxHandler;

//    HashSessionIdManager sessionIdManager;

	public void activeInit() throws Exception {
		jetty = new Server();
		handlers = new HandlerList();

		super.activeInit();

		jetty.setHandler(handlers);
		jetty.start();
	}

	public void activeRelease() throws Exception {
		if ( null != jetty ) {
			super.activeRelease();

			Server j = jetty;
			jetty = null;
			handlers = null;
			ctxHandler = null;

//            sessionIdManager = null;

			j.stop();
		}
	}

	@Override
	protected void initConnectorSsl(int portSsl) {
		HttpConfiguration https = new HttpConfiguration();
		https.addCustomizer(new SecureRequestCustomizer());

		// Configuring SSL
		SslContextFactory sslContextFactory = new SslContextFactory();

//        String str;
//        str = DustUtils.getCtxVal(ContextRef.self, DustNetAtts.NetSslInfoStorePath, false);
//        sslContextFactory.setKeyStorePath(ClassLoader.getSystemResource(str).toExternalForm());
//        str = DustUtils.getCtxVal(ContextRef.self, DustNetAtts.NetSslInfoStorePass, false);
//        sslContextFactory.setKeyStorePassword(str);
//        str = DustUtils.getCtxVal(ContextRef.self, DustNetAtts.NetSslInfoManagerPass, false);
//        sslContextFactory.setKeyManagerPassword(str);

		ServerConnector sslConnector = new ServerConnector(jetty, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
		sslConnector.setPort(portSsl);

		jetty.addConnector(sslConnector);
	}

	@Override
	protected void initConnectorPublic(int portPublic, int portSsl) {
		HttpConfiguration http = new HttpConfiguration();

		if ( NO_PORT_SET != portSsl ) {
			http.addCustomizer(new SecureRequestCustomizer());
			http.setSecurePort(portSsl);
			http.setSecureScheme("https");
		}

		ServerConnector connector = new ServerConnector(jetty);
		connector.addConnectionFactory(new HttpConnectionFactory(http));
		connector.setPort(portPublic);

		jetty.addConnector(connector);
	}

	public void initConnectors() throws Exception {
		initConnectorPublic(8080, NO_PORT_SET);
	}

	protected void initHandlers() {
		addServlet("/admin/*", new DustHttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void processRequest(Map data) throws Exception {

				Commands cmd;
				String str = Dust.access(data, MindAccess.Peek, Commands.info.name(), ServletData.Command);
				try {
					cmd = Commands.valueOf(str);
				} catch (Throwable e) {
					cmd = Commands.info;
				}

				switch ( cmd ) {
				case stop:
					new Thread() {
						@Override
						public void run() {
							try {
								activeRelease();
//  								DustGenLog.log("Shutting down Jetty server...");
//  								server.stop();
//  								DustGenLog.log("Jetty server shutdown OK.");
							} catch (Exception ex) {
//  								DustGenLog.log(DustEventLevel.ERROR, "Failed to stop Jetty");
							}
						}
					}.start();
					break;
				case info:
					Properties pp = System.getProperties();

					StringBuilder sb = new StringBuilder("<!doctype html>\n" + "<html lang=\"en\">\n" + "<head>\n<meta charset=\"utf-8\">\n<title>Hello World Server</title>\n</head>\n" + "<body>");

					sb.append("<h2>Server info</h2>");
					sb.append("<ul>");

					for (Object o : pp.keySet()) {
						String key = o.toString();
						sb.append("<li>" + key + ": " + pp.getProperty(key) + "</li>");
					}

					sb.append("</ul>");

					sb.append("<h2>Commands</h2>");
					sb.append("<ul>");
					for (Commands cc : Commands.values()) {
						sb.append("<li><a href=\"/admin/" + cc + "\">" + cc + "</a></li>");
					}
					sb.append("</ul>");

					PrintWriter out = getWriter(data);
					out.println(sb.toString());

					break;

				}

			}
		});
	}

	@Override
	public void addServlet(String path, HttpServlet servlet) {
		if ( null == ctxHandler ) {
			ctxHandler = new ServletContextHandler();
			ctxHandler.setContextPath("/*");
			handlers.addHandler(ctxHandler);
		}

		ctxHandler.addServlet(new ServletHolder(servlet), path);
	}
	
	public static void main(String[] args) throws Exception {
		DustHttpServerJetty srv = new DustHttpServerJetty();
		srv.activeInit();
	}

}
