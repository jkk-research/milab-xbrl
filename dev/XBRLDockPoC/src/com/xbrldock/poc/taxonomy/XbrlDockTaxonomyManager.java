package com.xbrldock.poc.taxonomy;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.XbrlDockPocConsts;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings("rawtypes")
public class XbrlDockTaxonomyManager implements XbrlDockTaxonomyConsts, XbrlDockPocConsts.XDModTaxonomyManager {

	private XDModUrlResolver urlCache;
	File taxonomyStoreRoot;
	File dirInput;

	private final Map<String, XbrlDockTaxonomy> taxonomies = new TreeMap<>();
	
	public XbrlDockTaxonomyManager() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void initModule(GenApp app, Map config) throws Exception {
		this.urlCache = app.getModule(XDC_CFGTOKEN_MOD_urlCache);
		String dataRoot = XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirStore);
		this.taxonomyStoreRoot = new File(dataRoot);
		XbrlDockUtilsFile.ensureDir(taxonomyStoreRoot);
		
		String inputRoot = XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirInput);
		this.dirInput = new File(inputRoot);
	}

//	public XbrlDockTaxonomyManager(String dataRoot, String cacheRoot) throws Exception {
//		urlCache = new XbrlDockDevUrlCache(cacheRoot);
//		XbrlDockUtilsXml.setDefEntityResolver(urlCache);
//		
//		taxonomyStoreRoot = new File(dataRoot);
//		XbrlDockUtilsFile.ensureDir(taxonomyStoreRoot);
//	}

	@Override
	public void importTaxonomy(File taxSource) throws Exception {
		File fTempDir = null;
		
		if ( taxSource.isFile() && taxSource.getName().endsWith(XDC_FEXT_ZIP)) {
			fTempDir = new File(dirInput, XbrlDockUtils.strTime());
			XbrlDockUtilsFile.extractWithApacheZipFile(fTempDir, taxSource, null);
			
			taxSource = fTempDir;
		}
		
		if ( taxSource.isDirectory() ) {
			
		}
		
	}
	
	public XbrlDockTaxonomy loadTaxonomy(String taxonomyId) throws Exception {
		
		File fMetaInf = new File(urlCache.getCacheRoot(), taxonomyId + "/" + XDC_FNAME_METAINF);
		
		File fTax = new File(fMetaInf, XDC_FNAME_TAXPACK);
		Element eTaxPack = XbrlDockUtilsXml.parseDoc(fTax).getDocumentElement();

		Node eTP = eTaxPack.getElementsByTagName("tp:identifier").item(0);
		String id = eTP.getTextContent().trim();
		
		String taxId = XbrlDockUtils.getPostfix(id, XDC_URL_PSEP);
		
		XbrlDockTaxonomy ret = XbrlDockUtils.safeGet(taxonomies, taxId, new XbrlDockUtils.ItemCreator<XbrlDockTaxonomy>() {
			@Override
			public XbrlDockTaxonomy create(Object key, Object... hints) {
				try {
					XbrlDockTaxonomy t = new XbrlDockTaxonomy(taxId, fMetaInf, XbrlDockTaxonomyManager.this);
//					t.loadTaxonomy(fMetaInf);
					return t;
				} catch (Exception e) {
					return XbrlDockException.wrap(e, "Taxonomy load error", key);
				}
			}
		});
		
		return ret;
	}

	public InputStream getUrlStream(String url) throws Exception {
		return urlCache.resolveEntityStream("", url);
	}

	public XDModUrlResolver getUrlCache() {
		return urlCache;
	}

}
