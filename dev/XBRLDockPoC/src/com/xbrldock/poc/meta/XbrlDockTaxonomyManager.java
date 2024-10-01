package com.xbrldock.poc.meta;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;
import com.xbrldock.poc.utils.XbrlDockPocUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsJson;
import com.xbrldock.utils.XbrlDockUtilsNet;
import com.xbrldock.utils.XbrlDockUtilsXml;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockTaxonomyManager implements XbrlDockMetaConsts, XbrlDockConsts.GenAgent {

	File taxonomyStoreRoot;
	File dirInput;

	private final Map<String, XbrlDockMetaTaxonomy> taxonomies = new TreeMap<>();

	public XbrlDockTaxonomyManager() {
	}

	@Override
	public void initModule(GenApp app, Map config) throws Exception {
		String dataRoot = XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirStore);
		this.taxonomyStoreRoot = new File(dataRoot);
		XbrlDockUtilsFile.ensureDir(taxonomyStoreRoot);

		String inputRoot = XbrlDockUtils.simpleGet(config, XDC_CFGTOKEN_dirInput);
		this.dirInput = new File(inputRoot);
	}

	@Override
	public <RetType> RetType process(String command, Object... params) throws Exception {
		Object ret = null;
		switch (command) {
		case XDC_CMD_METAMGR_IMPORT:
			importTaxonomy((File) params[0]);
			break;
		default:
			XbrlDockException.wrap(null, "Unhandled agent command", command, params);
			break;
		}
		
		return (RetType) ret;
	}

	private void importTaxonomy(File taxSource) throws Exception {
		File fTempDir = null;

		if (taxSource.isFile() && taxSource.getName().endsWith(XDC_FEXT_ZIP)) {
			fTempDir = new File(dirInput, XbrlDockUtils.strTime());
			XbrlDockUtilsFile.extractWithApacheZipFile(fTempDir, taxSource, null);

			taxSource = fTempDir;
		}

		if (taxSource.isDirectory()) {
			Map txmyInfo = XbrlDockPocUtils.readMeta(taxSource, null);
			
			String id = XbrlDockUtils.simpleGet(txmyInfo, XDC_METAINFO_pkgInfo, "identifier");
			id = XbrlDockUtils.getPostfix(id, XDC_URL_PSEP);
			File fDir = new File(taxonomyStoreRoot, id);
			XbrlDockUtilsFile.ensureDir(fDir);
			
			XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMY_FNAME), txmyInfo);
			
			XbrlDock.log(EventLevel.Trace, txmyInfo);
		}

	}

	public XbrlDockMetaTaxonomy loadTaxonomy(String taxonomyId) throws Exception {

		File fMetaInf = new File(XbrlDockUtilsNet.getCacheRoot(), taxonomyId + "/" + XDC_FNAME_METAINF);

		File fTax = new File(fMetaInf, XDC_FNAME_TAXPACK);
		Element eTaxPack = XbrlDockUtilsXml.parseDoc(fTax).getDocumentElement();

		Node eTP = eTaxPack.getElementsByTagName("tp:identifier").item(0);
		String id = eTP.getTextContent().trim();

		String taxId = XbrlDockUtils.getPostfix(id, XDC_URL_PSEP);

		XbrlDockMetaTaxonomy ret = XbrlDockUtils.safeGet(taxonomies, taxId, new XbrlDockUtils.ItemCreator<XbrlDockMetaTaxonomy>() {
			@Override
			public XbrlDockMetaTaxonomy create(Object key, Object... hints) {
				try {
					XbrlDockMetaTaxonomy t = new XbrlDockMetaTaxonomy(taxId, fMetaInf, XbrlDockTaxonomyManager.this);
//					t.loadTaxonomy(fMetaInf);
					return t;
				} catch (Exception e) {
					return XbrlDockException.wrap(e, "Taxonomy load error", key);
				}
			}
		});

		return ret;
	}

}
