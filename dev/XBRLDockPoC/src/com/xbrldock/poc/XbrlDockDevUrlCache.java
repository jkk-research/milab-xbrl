package com.xbrldock.poc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.xbrldock.XbrlDockException;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsNet;

public class XbrlDockDevUrlCache implements EntityResolver {
		public final File cacheRoot;
		
		public XbrlDockDevUrlCache(String cacheRoot) {
			this.cacheRoot = new File(cacheRoot);
		}

		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			return new InputSource(resolveEntityStream(publicId, systemId));
		}

		public InputStream resolveEntityStream(String publicId, String systemId) throws SAXException, IOException {
			String path = XbrlDockUtils.getPostfix(systemId, "://");
			
			File fCache = new File(cacheRoot, path);
			
			if ( !fCache.isFile() ) {
				XbrlDockUtilsFile.ensureDir(fCache.getParentFile());
				try {
					XbrlDockUtilsNet.download(systemId, fCache);
				} catch (Exception e) {
					XbrlDockException.wrap(e, "Downloading", systemId);
				}
			}
			
			if ( fCache.isFile() ) {
				return new FileInputStream(fCache);
			}

			return null;
		}
	}