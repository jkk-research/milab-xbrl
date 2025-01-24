package com.xbrldock.poc.meta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Element;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevCounter;
import com.xbrldock.poc.utils.XbrlDockPocUtils;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsFile;
import com.xbrldock.utils.XbrlDockUtilsJson;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlDockMetaContainer implements XbrlDockMetaConsts {

	final XbrlDockMetaManager metaManager;
	final Set<XbrlDockMetaContainer> requires = new HashSet<>();

	final Map<String, Object> metaInfo;

	Set<String> ownedUrls = new TreeSet<>();
	private Set<String> loaded = new TreeSet<>();

	Map<String, Map<String, Object>> contentByURL = new TreeMap<>();
	Map references = new TreeMap<>();
	Map<String, Map<String, Object>> labels = new TreeMap<>();
	Map<String, Object> itemsByNS = new TreeMap<>();

	Map<String, Map<String, RoleTree>> roleTreeMap;

	boolean updated;

	private ArrayList<String> queue = new ArrayList<>();
	Map<String, String> queueNS = new TreeMap<>();
	String currentUrl;
	String currentNS;
	String path;
	Map currentContent;

	XbrlDockDevCounter cntLinkTypes = new XbrlDockDevCounter("LinkTypeCounts", true);
	XbrlDockDevCounter cntArcRoles = new XbrlDockDevCounter("ArcRoleCounts", true);

	public XbrlDockMetaContainer(XbrlDockMetaManager mm, String id) {
		metaManager = mm;
		metaInfo = new TreeMap<String, Object>();
		updated = false;

		XbrlDockUtils.simpleSet(metaInfo, id, XDC_METAINFO_pkgInfo, XDC_EXT_TOKEN_identifier);
		addOwnedUrl(id);
	}

	public XbrlDockMetaContainer(XbrlDockMetaManager mm, Map<String, Object> mi) {
		metaManager = mm;
		metaInfo = mi;
		updated = false;

		Map rewrite = XbrlDockUtils.simpleGet(metaInfo, XDC_METAINFO_urlRewrite);
		if (null != rewrite) {
			for (Object s : rewrite.keySet()) {
				addOwnedUrl(s);
			}
		}
	}

	private boolean addOwnedUrl(Object s) {
		return ownedUrls.add(XbrlDockUtils.getPostfix((String) s, XDC_URL_PSEP));
	}

	public Map getUrlContent(String url) {
		String key = XbrlDockUtils.getPostfix(url, XDC_URL_PSEP);
		return contentByURL.get(key);
	}

	public Map getMetaInfo() {
		return metaInfo;
	}

	public Iterable<String> getRoleTypes() {
		getRoleTreeMap("");
		return roleTreeMap.keySet();
	}

	public synchronized Map<String, RoleTree> getRoleTreeMap(String type) {
		if (null == roleTreeMap) {
			roleTreeMap = new TreeMap<>();
			Map<String, Map> roles = new TreeMap<String, Map>();

			visit(XDC_METATOKEN_items, new GenAgent() {

				@Override
				public Object process(String cmd, Map params) throws Exception {
					switch (cmd) {
					case XDC_CMD_GEN_Process:
						Map<String, String> l = ((Map.Entry<String, Map>) params.get(XDC_EXT_TOKEN_value)).getValue();

						String roleUri = l.get("roleURI");

						if (!XbrlDockUtils.isEmpty(roleUri)) {
							roles.put(XbrlDockUtils.getPostfix(roleUri, XDC_URL_PSEP), l);
						}

						break;
					default:
						break;
					}
					return true;
				}
			});

			visit(XDC_METATOKEN_links, new GenAgent() {
				String url;
				Set<RoleTree> localTrees = new HashSet<>();

				ItemCreator<RoleTreeNode> crtNode = new ItemCreator<RoleTreeNode>() {
					@Override
					public RoleTreeNode create(Object key, Object... hints) {
						Map item = peekItem((String) key);
						return new RoleTreeNode(url, item);
					}
				};

				ItemCreator<RoleTree> crtTree = new ItemCreator<RoleTree>() {
					@Override
					public RoleTree create(Object key, Object... hints) {
						Map item = roles.get((String) key);
						return new RoleTree(url, item);
					}
				};

				@Override
				public Object process(String cmd, Map params) throws Exception {
					switch (cmd) {
					case XDC_CMD_GEN_Begin:
						url = (String) params.get(XDC_EXT_TOKEN_id);
						localTrees.clear();
						break;
					case XDC_CMD_GEN_Process:
						Map<String, String> l = (Map) params.get(XDC_EXT_TOKEN_value);

						String roleType = l.get("type");
						Map<String, RoleTree> typeMap = XbrlDockUtils.safeGet(roleTreeMap, roleType, SORTEDMAP_CREATOR);

						String roleName = l.get("xlink:role");
						RoleTree rt = XbrlDockUtils.safeGet(typeMap, roleName, crtTree);

						localTrees.add(rt);

						RoleTreeNode rtnFrom = XbrlDockUtils.safeGet(rt.allNodes, l.get("xlink:from"), crtNode);
						RoleTreeNode rtnTo = XbrlDockUtils.safeGet(rt.allNodes, l.get("xlink:to"), crtNode);

						rtnTo.setUpLink(l);

						rtnFrom.addChild(rtnTo);
						break;
					case XDC_CMD_GEN_End:
						for (RoleTree lt : localTrees) {
							for (Map.Entry<String, RoleTreeNode> lne : lt.allNodes.entrySet()) {
								RoleTreeNode ne = lne.getValue();
								if (null == ne.getUpLink()) {
									lt.addChild(ne);
								}
							}
						}
						break;
					default:
						break;
					}
					return true;
				}
			});
		}

		return roleTreeMap.get(type);
	}

	void setUrlContent(Map content) throws Exception {
		String key = XbrlDockUtils.getPostfix(currentUrl, XDC_URL_PSEP);
		contentByURL.put(key, content);

		Map<String, Map<String, Object>> items = (Map<String, Map<String, Object>>) content.getOrDefault(XDC_METATOKEN_items, Collections.EMPTY_MAP);
		for (Map.Entry<String, Map<String, Object>> ei : items.entrySet()) {
			registerNSItem(ei.getValue().get("name"), ei.getKey());
		}

		Map<String, String> incl = (Map) content.getOrDefault(XDC_METATOKEN_includes, Collections.EMPTY_MAP);
		for (Map.Entry<String, String> ie : incl.entrySet()) {
			optQueue(ie.getKey(), ie.getValue());
		}
	}

	private void registerNSItem(Object name, Object id) {
		itemsByNS.put(currentNS + ":" + name, XbrlDockUtils.sbAppend(null, ":", true, currentUrl, XDC_METATOKEN_items, id).toString());
	}

	Map getItem(Element e) throws Exception {
		String itemRef = e.getAttribute("xlink:href");

		int sp = itemRef.lastIndexOf("#");
		String id = itemRef.substring(sp + 1);
		String realRef = itemRef.substring(0, sp);

		realRef = XbrlDockUtils.optCleanUrl(realRef);

		return getItem(realRef, id, null);
	}

	Map getItem(String id, String callerNamespace) throws Exception {
		return getItem(currentUrl, id, callerNamespace);
	}

	Map getItem(String itemUrl, String id, String callerNamespace) throws Exception {
		String realUrl = XbrlDockUtils.optExtendRef(itemUrl, path);

		optQueue(realUrl, callerNamespace);

		String key = XbrlDockUtils.getPostfix(realUrl, XDC_URL_PSEP);

		Map m = metaManager.getKnownItemForKey(key, id, this);

		if (null == m) {
			m = XbrlDockUtils.safeGet(contentByURL, key, MAP_CREATOR);
			m = XbrlDockUtils.safeGet(m, XDC_METATOKEN_items, MAP_CREATOR);
			m = XbrlDockUtils.safeGet(m, id, MAP_CREATOR);

			m.put(XDC_METATOKEN_url, realUrl);
			m.put("id", id);
		} else {
//			XbrlDock.log(EventLevel.Trace, "External item resolved", m);
		}

		updated = true;

		return m;
	}

	String getQueuedItem() {
		if (queue.isEmpty()) {
			return null;
		}

		return setCurrentUrl(queue.remove(0));
	}

	String setCurrentUrl(String url) {
		currentUrl = url;
		loaded.add(currentUrl);

		path = XbrlDockUtils.cutPostfix(currentUrl, "/");
		currentNS = queueNS.remove(currentUrl);

		String key = XbrlDockUtils.getPostfix(currentUrl, XDC_URL_PSEP);
		currentContent = XbrlDockUtils.safeGet(contentByURL, key, MAP_CREATOR);

		return currentUrl;
	}

	String getCurrentUrl() {
		return currentUrl;
	}

	boolean optQueue(String url, String targetNs) {
		url = XbrlDockUtils.optExtendRef(url, path);

		if (null != currentContent) {
			Map incl = XbrlDockUtils.safeGet(currentContent, XDC_METATOKEN_includes, SORTEDMAP_CREATOR);
			incl.put(url, targetNs);
		}

//		if ((null != currentUrl) && !XbrlDockUtils.isEqual(url, currentUrl)) {
//			XbrlDockUtils.safeGet(fileLinks, XbrlDockUtils.getPostfix(currentUrl, XDC_URL_PSEP), SORTEDSET_CREATOR).add(XbrlDockUtils.getPostfix(url, XDC_URL_PSEP));
//		}

		if (loaded.contains(url) || queue.contains(url)) {
			return false;
		}

		int idx = -1;
		if (url.endsWith(XDC_FEXT_XML)) {
			idx = queue.size();
		} else if (url.endsWith(XDC_FEXT_SCHEMA)) {
			idx = 0;
		} else {
			XbrlDock.log(EventLevel.Warning, "Strange extension", url);
		}

		if (-1 != idx) {
			queue.add(idx, url);

			if (!XbrlDockUtils.isEmpty(targetNs)) {
				queueNS.put(url, targetNs);
			}
			return true;
		}

		return false;
	}

	int storeDocumentRef(Map rm) {
		ArrayList<Map> allRefs = XbrlDockUtils.safeGet(references, XDC_METATOKEN_references, ARRAY_CREATOR);
		allRefs.add(rm);
		return allRefs.size() - 1;
	}

	void setDocumentRef(Map item, Object refIdx) {
		String itemId = XbrlDockPocUtils.getGlobalItemId(item);
		Map refLinks = XbrlDockUtils.safeGet(references, XDC_METATOKEN_refLinks, SORTEDMAP_CREATOR);

		ArrayList al = XbrlDockUtils.safeGet(refLinks, itemId, ARRAY_CREATOR);

		if (!al.contains(refIdx)) {
			al.add(refIdx);
			updated = true;
		}
	}

	void addLinkType(String lt) {
		cntLinkTypes.add(lt);
	}

	void addArcRole(String ar) {
		cntArcRoles.add(ar);
		cntArcRoles.add(" <TOTAL> ");
	}

	void setLabel(Object lang, Object itemId, String labelType, Object value) {
		Map ll = XbrlDockUtils.safeGet(labels, lang, SORTEDMAP_CREATOR);
		XbrlDockUtils.simpleSet(ll, value, itemId, labelType);
		updated = true;
	}

	void addLink(Map<String, String> linkInfo) {
		ArrayList<Map<String, String>> links = XbrlDockUtils.safeGet(currentContent, XDC_METATOKEN_links, ARRAY_CREATOR);
		links.add(linkInfo);
		updated = true;
	}

	public boolean optSave() throws Exception {
		if (updated) {
			String id = getId();
			File fDir = new File(metaManager.metaStoreRoot, id);
			XbrlDockUtilsFile.ensureDir(fDir);

			XbrlDock.log(EventLevel.Trace, "Saving MetaContainer", id);

			metaInfo.remove(XDC_METAINFO_dir);
			ArrayList inc = new ArrayList(loaded);
			metaInfo.put(XDC_METATOKEN_includes, inc);

			Set reqIds = new TreeSet();
			for (XbrlDockMetaContainer m : requires) {
				if (m != this) {
					reqIds.add(m.getId());
				}
			}

			if (!reqIds.isEmpty()) {
				metaInfo.put(XDC_GEN_TOKEN_requires, new ArrayList(reqIds));
			}

			metaInfo.put(XDC_METAINFO_ownedUrls, new ArrayList(ownedUrls));
			metaInfo.put(XDC_FACT_TOKEN_language, new ArrayList(labels.keySet()));

			metaInfo.put(XDC_METAINFO_arcRoles, new ArrayList(cntArcRoles.keys()));
			metaInfo.put(XDC_METAINFO_linkTypes, new ArrayList(cntLinkTypes.keys()));

//			Map<String, ArrayList<String>> mfl = XbrlDockUtils.safeGet(metaInfo, XDC_METATOKEN_fileLinks, SORTEDMAP_CREATOR);
//			for ( Map.Entry<String, Set<String>> efl : fileLinks.entrySet() ) {
//				mfl.put(efl.getKey(), new ArrayList(efl.getValue()));
//			}

			CounterProcessor cnt = new CounterProcessor();

			visit(XDC_METATOKEN_items, cnt);
			metaInfo.put(XDC_METATOKEN_items, cnt.getCount());
			cnt.process(XDC_CMD_GEN_Init, null);

			visit(XDC_METATOKEN_links, cnt);
			metaInfo.put(XDC_METATOKEN_links, cnt.getCount());
			cnt.process(XDC_CMD_GEN_Init, null);

			visit(XDC_METATOKEN_references, cnt);
			metaInfo.put(XDC_METATOKEN_references, cnt.getCount());
			cnt.process(XDC_CMD_GEN_Init, null);

			XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYHEAD_FNAME), metaInfo);
			XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYDATA_FNAME), contentByURL);
			XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYREFS_FNAME), references);

			for (Entry<String, Map<String, Object>> le : labels.entrySet()) {
				XbrlDockUtilsJson.writeJson(new File(fDir, XDC_TAXONOMYRES_FNAME_PREFIX + le.getKey() + XDC_FEXT_JSON), le.getValue());
			}

			updated = false;

			return true;
		}

		return false;
	}

	public Map<String, String> getRes(String lang, Map item) throws Exception {
		Map<String, Object> res = XbrlDockUtils.safeGet(labels, lang, new ItemCreator<Map>() {
			@Override
			public Map create(Object key, Object... hints) {
				String id = getId();
				File fDir = new File(metaManager.metaStoreRoot, id);
				File fRes = new File(fDir, XDC_TAXONOMYRES_FNAME_PREFIX + key + XDC_FEXT_JSON);

				Map m = Collections.EMPTY_MAP;

				if (fRes.isFile()) {
					try {
						m = XbrlDockUtilsJson.readJson(fRes);
					} catch (Exception e) {
					}
				}

				return m;
			}
		});

		String id = XbrlDockUtils
				.sbAppend(null, "#", false, XbrlDockUtils.getPostfix((String) item.get(XDC_METATOKEN_url), XDC_URL_PSEP), item.get(XDC_EXT_TOKEN_id)).toString();

		return (Map<String, String>) res.getOrDefault(id, Collections.EMPTY_MAP);
	}

	public boolean load() throws Exception {
		String id = getId();
		File fDir = new File(metaManager.metaStoreRoot, id);

		if (fDir.isDirectory()) {
//		metaInfo.putAll(XbrlDockUtilsJson.readJson(new File(fDir, XDC_TAXONOMYHEAD_FNAME)));
			contentByURL.putAll(XbrlDockUtilsJson.readJson(new File(fDir, XDC_TAXONOMYDATA_FNAME)));
			references.putAll(XbrlDockUtilsJson.readJson(new File(fDir, XDC_TAXONOMYREFS_FNAME)));

			loadSet(ownedUrls, XDC_METAINFO_ownedUrls);
			loadSet(loaded, XDC_METATOKEN_includes);
//			requires.addAll(XbrlDockUtils.simpleGet(metaInfo, XDC_GEN_TOKEN_requires));

//			Map<String, ArrayList<String>> fl = (Map<String, ArrayList<String>>) metaInfo.getOrDefault(XDC_METATOKEN_fileLinks, Collections.EMPTY_MAP);
//			fileLinks.clear();
//			for (Map.Entry<String, ArrayList<String>> fle : fl.entrySet()) {
//				fileLinks.put(fle.getKey(), new TreeSet<String>(fle.getValue()));
//			}

			updated = false;

			return true;
		} else {
			return false;
		}
	}

	private void loadSet(Collection c, String metaKey) {
		c.clear();
		Collection src = XbrlDockUtils.simpleGet(metaInfo, metaKey);
		if (null != src) {
			c.addAll(src);
		}
	}

	public String getId() {
		String id = XbrlDockUtils.simpleGet(metaInfo, XDC_METAINFO_pkgInfo, XDC_EXT_TOKEN_identifier);
		id = XbrlDockUtils.getPostfix(id, XDC_URL_PSEP);
		return id;
	}

	public Map peekItem(String id) {
		Map m = null;
		int sp = id.indexOf(XDC_SEP_ITEMID);

		if (-1 != sp) {
			String fileId = id.substring(0, sp);
			String itemId = id.substring(sp + XDC_SEP_ITEMID.length());
			m = metaManager.getKnownItemForKey(fileId, itemId, this);

			if (null == m) {
				m = XbrlDockUtils.simpleGet(contentByURL, fileId, XDC_METATOKEN_items, itemId);
			}
		}
		return m;
//		return (null == m) ? Collections.EMPTY_MAP : m;
	}

	public String getItemLabel(String id) {
		return XbrlDockUtils.getPostfix(id, XDC_SEP_ITEMID);
	}

	public int collectLinks(Set<String> target, String url) {
		if (target.add(url)) {
			Map<String, String> incl = XbrlDockUtils.simpleGet(contentByURL, url, XDC_METATOKEN_includes);

//			Set<String> links = fileLinks.get(url);

			if (null != incl) {
				for (String l : incl.keySet()) {
					collectLinks(target, l);
				}
			} else {
				for (XbrlDockMetaContainer mc : requires) {
					if (null != mc) {
						mc.collectLinks(target, url);
					}
				}
			}
		}

		return target.size();
	}

	public void visit(String itemType, GenAgent lp, Object... params) {
		Map m = null;
		Collection c;

		try {

			switch (itemType) {
			case XDC_METATOKEN_items:
				for (Map ce : contentByURL.values()) {
					m = (Map) ce.getOrDefault(itemType, Collections.EMPTY_MAP);
					if ((null != m) && !m.isEmpty()) {
						lp.process(XDC_CMD_GEN_Begin, null);
						for (Object e : m.entrySet()) {
							lp.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParams(XDC_EXT_TOKEN_value, e));
						}
						lp.process(XDC_CMD_GEN_End, null);
					}
				}
				break;
			case XDC_METATOKEN_links:
				for (Map.Entry<String, Map<String, Object>> ce : contentByURL.entrySet()) {
					c = (Collection) ce.getValue().getOrDefault(itemType, Collections.EMPTY_LIST);
					if ((null != c) && !c.isEmpty()) {
						String key = ce.getKey();
						lp.process(XDC_CMD_GEN_Begin, XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, key));
						for (Object e : c) {
							lp.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParams(XDC_EXT_TOKEN_value, e));
						}
						lp.process(XDC_CMD_GEN_End, XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, key));
					}
				}
				break;
			case XDC_METATOKEN_references:
				c = (Collection) references.getOrDefault(itemType, Collections.EMPTY_LIST);
				if ((null != c) && !c.isEmpty()) {
					lp.process(XDC_CMD_GEN_Begin, null);
					for (Object e : c) {
						lp.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParams(XDC_EXT_TOKEN_value, e));
					}
					lp.process(XDC_CMD_GEN_End, null);
				}
				break;
			case XDC_METATOKEN_refLinks:
				m = (Map) references.getOrDefault(itemType, Collections.EMPTY_MAP);
				if ((null != m) && !m.isEmpty()) {
					for (Object e : m.entrySet()) {
						Map.Entry me = (Map.Entry) e;
						Object key = me.getKey();
						lp.process(XDC_CMD_GEN_Begin,  XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, key));
						for (Object v : (Collection) me.getValue()) {
							lp.process(XDC_CMD_GEN_Process,  XbrlDockUtils.setParams(XDC_EXT_TOKEN_value, v));
						}
						lp.process(XDC_CMD_GEN_End,  XbrlDockUtils.setParams(XDC_EXT_TOKEN_id, key));
					}
				}

				break;
			case XDC_METATOKEN_labels:
				m = XbrlDockUtils.simpleGet(labels, params[1], params[0]);
				break;
			default:
				XbrlDockException.wrap(null, "Unknown visit item type", getId(), itemType);
			}

			if ((null != m) && !m.isEmpty()) {
				lp.process(XDC_CMD_GEN_Begin, null);
				for (Object e : m.entrySet()) {
					lp.process(XDC_CMD_GEN_Process, XbrlDockUtils.setParams(XDC_EXT_TOKEN_value, e));
				}
				lp.process(XDC_CMD_GEN_End, null);
			}

		} catch (Throwable e) {
			XbrlDockException.wrap(e, "MetaContainer visit processor exception", getId(), itemType, params);
		}

	}

	public void optConvertRules(ArrayList<Map<String, Object>> ruleArr) {

		Map formula = (Map) metaInfo.get(XDC_METATOKEN_formula);

		if (null != formula) {
			ArrayList<Map<String, Object>> fArr = new ArrayList<>(((Collection<Map>) formula.getOrDefault(XDC_FORMULA_assertions, Collections.EMPTY_LIST)));
			fArr.addAll(((Collection<Map<String, Object>>) formula.getOrDefault(XDC_FORMULA_expressions, Collections.EMPTY_LIST)));

			for (Map fObj : fArr) {
				String str;

				str = (String) fObj.get(XDC_FORMULA_formula);
				boolean validation = XbrlDockUtils.isEmpty(str);
				if (!validation) {
					str = str.replaceAll("\\$", "");
					str = str.replaceAll(" div ", " / ");
					fObj.put(XDC_UTILS_MVEL_mvelText, str);
					
					XbrlDock.log(EventLevel.Trace, "Expression", str);
				}

				str = (String) fObj.get(XDC_FORMULA_condition);
				if (!XbrlDockUtils.isEmpty(str)) {
					str = str.replace("$", "");
					str = str.replace("=", "==");
					str = str.replaceAll("<\\s*>", "!=");
					str = str.replace(" and ", " && ");
					str = str.replace(" or ", " || ");
					
					if ( str.contains("\"null\"")) {
						str = str.replaceAll("\\(\\s*(\\w+)\\s+eq\\s+\"null\"\\s*\\)", "!exists\\($1\\)");
					}

					if ( str.contains("exists(")) {
						str = str.replaceAll("exists\\s*\\(\\s*(\\w+)\\s*\\)", "xdgen.exists\\(\"$1\"\\)");
					}

					fObj.put(validation ? XDC_UTILS_MVEL_mvelText : XDC_UTILS_MVEL_mvelCondition, str);
					
					XbrlDock.log(EventLevel.Trace, "Condition", str);
				}

				fObj.put(XDC_UTILS_MVEL_mvelType, validation ? XDC_UTILS_MVEL_mvelTypeValidation : XDC_UTILS_MVEL_mvelTypeCalculation);


				str = (String) fObj.get(XDC_FACT_TOKEN_concept);
				if (XbrlDockUtils.isEmpty(str)) {
					fObj.put(XDC_FACT_TOKEN_concept, fObj.get(XDC_EXT_TOKEN_id));
				}
			}
			ruleArr.addAll(fArr);
			
		}
	}
}
