package hu.sze.uni.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustConsts.MindAccess;

@SuppressWarnings("rawtypes")
public abstract class DustHttpServlet extends HttpServlet implements DustHttpConsts {
	private static final long serialVersionUID = 1L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Enumeration<String> ee;
		String n = null;

		Map data = new HashMap();

		try {
			for (ee = request.getAttributeNames(); ee.hasMoreElements();) {
				n = ee.nextElement();
				Dust.access(data, MindAccess.Set, request.getAttribute(n), ServletData.Attribute, n);
			}

			for (ee = request.getParameterNames(); ee.hasMoreElements();) {
				n = ee.nextElement();
				Dust.access(data, MindAccess.Set, request.getParameter(n), ServletData.Parameter, n);
			}

			for (ee = request.getHeaderNames(); ee.hasMoreElements();) {
				n = ee.nextElement();
				Dust.access(data, MindAccess.Set, request.getHeader(n), ServletData.Header, n);
			}

			String str = request.getPathInfo();
			if ( null != str ) {
				int idx = str.lastIndexOf("/");
				if ( -1 != idx ) {
					str = str.substring(idx + 1);
				}

				Dust.access(data, MindAccess.Set, str, ServletData.Command);
			}

			Dust.access(data, MindAccess.Set, request.getMethod(), ServletData.Method);
			Dust.access(data, MindAccess.Set, CHARSET_UTF8, ServletData.Charset);
			Dust.access(data, MindAccess.Set, CONTENT_JSON, ServletData.ContentType);

			Dust.access(data, MindAccess.Set, HttpServletResponse.SC_OK, ServletData.Status);
			Dust.access(data, MindAccess.Set, response, ServletData.Response);

			processRequest(data);
			
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable t) {
			try {
				getWriter(data).close();
			} catch (Throwable e) {
				e.printStackTrace();
			}
			try {
				getOutStream(data).close();
			} catch (Throwable e) {
				e.printStackTrace();
			}
			
			throw new ServletException("Quick and dirty exception handling", t);
		}
	}
	
	protected PrintWriter getWriter(Map data) throws Exception {
		HttpServletResponse response = Dust.access(data, MindAccess.Peek, null, ServletData.Response);
		return response.getWriter();
	}

	protected OutputStream getOutStream(Map data) throws Exception {
		HttpServletResponse response = Dust.access(data, MindAccess.Peek, null, ServletData.Response);
		return response.getOutputStream();
	}

	protected abstract void processRequest(Map data) throws Exception;
}