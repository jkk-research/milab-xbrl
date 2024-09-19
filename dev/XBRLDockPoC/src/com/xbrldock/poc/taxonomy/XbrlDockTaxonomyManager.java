package com.xbrldock.poc.taxonomy;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.XbrlDockDevUrlCache;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsXml;

public class XbrlDockTaxonomyManager implements XbrlDockTaxonomyConsts {

	private final XbrlDockDevUrlCache urlCache;
	final File taxomonyStoreRoot;

	private final Map<String, XbrlDockTaxonomy> taxonomies = new TreeMap<>();

	public XbrlDockTaxonomyManager(String dataRoot, String cacheRoot) throws Exception {
		urlCache = new XbrlDockDevUrlCache(cacheRoot);
		XbrlDockUtilsXml.setDefEntityResolver(urlCache);
		
		taxomonyStoreRoot = new File(dataRoot);
		XbrlDockUtilsFile.ensureDir(taxomonyStoreRoot);
	}

	public XbrlDockTaxonomy loadTaxonomy(String taxonomyId, String... urls) throws Exception {
		
		File fMetaInf = new File(urlCache.cacheRoot, taxonomyId + "/" + XBRLDOCK_FNAME_METAINF);
		
		File fTax = new File(fMetaInf, XBRLDOCK_FNAME_TAXPACK);
		Element eTaxPack = XbrlDockUtilsXml.parseDoc(fTax).getDocumentElement();

		Node eTP = eTaxPack.getElementsByTagName("tp:identifier").item(0);
		String id = eTP.getTextContent().trim();
		
		String taxId = XbrlDockUtils.getPostfix(id, XBRLDOCK_URL_PSEP);
		
		XbrlDockTaxonomy ret = XbrlDockUtils.safeGet(taxonomies, taxId, new XbrlDockUtils.ItemCreator<XbrlDockTaxonomy>() {
			@Override
			public XbrlDockTaxonomy create(Object key, Object... hints) {
				try {
					XbrlDockTaxonomy t = new XbrlDockTaxonomy(taxId, fMetaInf, XbrlDockTaxonomyManager.this);
//					t.loadTaxonomy(fMetaInf, urls);
					return t;
				} catch (Exception e) {
					return XbrlDockException.wrap(e, "Taxonoly load error", key, urls);
				}
			}
		});
		
		return ret;
	}

	public InputStream resolveEntityStream(String mapId, String url) throws Exception {
		return urlCache.resolveEntityStream(mapId, url);
	}

}
