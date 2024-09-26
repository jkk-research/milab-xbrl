package com.xbrldock.poc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.xbrldock.XbrlDockException;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsNet;
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings("rawtypes")
public class XbrlDockDevUrlCache implements XbrlDockPocConsts, XbrlDockPocConsts.XDModUrlResolver {
	private File cacheRoot;

	private static ThreadLocal<Map<String, File>> URL_REWRITES = new ThreadLocal<Map<String, File>>() {
		@Override
		protected Map<String, File> initialValue() {
			return new TreeMap<String, File>(Collections.reverseOrder());
		}
	};

	public XbrlDockDevUrlCache() {
	};

	@Override
	public void initModule(GenApp app, Map config) {
		String cacheRoot = XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirStore);
		this.cacheRoot = new File(cacheRoot);
		XbrlDockUtilsXml.setDefEntityResolver(this);
	}

//	public XbrlDockDevUrlCache(String cacheRoot) {
//		this.cacheRoot = new File(cacheRoot);
//	}
	
	@Override
	public File getCacheRoot() {
		return cacheRoot;
	}

	@Override
	public void setRewrite(File root, Map<String, String> prefixes) throws Exception {
		Map<String, File> ur = URL_REWRITES.get();

		ur.clear();

		for (Map.Entry<String, String> ep : prefixes.entrySet()) {
			ur.put(ep.getKey(), new File(root, ep.getValue()).getCanonicalFile());
		}
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		return new InputSource(resolveEntityStream(publicId, systemId));
	}

	public InputStream resolveEntityStream(String publicId, String systemId) throws SAXException, IOException {
		String path = XbrlDockUtils.getPostfix(systemId, "://");
		File fCache = new File(cacheRoot, path);

		for (Map.Entry<String, File> ep : URL_REWRITES.get().entrySet()) {
			String p = ep.getKey();
			if (systemId.startsWith(p)) {
				fCache = new File(ep.getValue(), systemId.substring(p.length()));
				break;
			}
		}

		if (!fCache.isFile()) {
			XbrlDockUtilsFile.ensureDir(fCache.getParentFile());
			try {
				XbrlDockUtilsNet.download(systemId, fCache);
			} catch (Exception e) {
				XbrlDockException.wrap(e, "Downloading", systemId);
			}
		}

		if (fCache.isFile()) {
			return new FileInputStream(fCache);
		}

		return null;
	}
}