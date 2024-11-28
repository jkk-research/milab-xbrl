package com.xbrldock.poc.sandbox;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.xbrldock.XbrlDock;
import com.xbrldock.XbrlDockException;
import com.xbrldock.dev.XbrlDockDevCounter;
import com.xbrldock.poc.meta.XbrlDockMetaConsts;
import com.xbrldock.poc.meta.XbrlDockMetaContainer;
import com.xbrldock.utils.XbrlDockUtils;

@SuppressWarnings({ /*"rawtypes", */ "unchecked" })
public class XbrlDockESRSTest implements XbrlDockMetaConsts {

	public static void exportMetaContainer(XbrlDockMetaContainer mc) {
		XbrlDockDevCounter cntCols = new XbrlDockDevCounter("ItemCols", true);
		XbrlDockDevCounter cntLabels = new XbrlDockDevCounter("ItemLabels", true);

		Map<Object, Map<String, String>> itemMap = new TreeMap<>();

		mc.visit(XDC_METATOKEN_items, new GenAgent() {
			@Override
			public Object process(String cmd, Object... params) throws Exception {
				switch (cmd) {
				case XDC_CMD_GEN_Process:
					Map.Entry<String, Map<String, String>> ie = (Map.Entry<String, Map<String, String>>) params[0];
					Map<String, String> origItem = ie.getValue();

					String sg = origItem.get("substitutionGroup");

					if (!XbrlDockUtils.isEmpty(sg)) {
//						XbrlDock.log(EventLevel.Info, item);

						Map<String, String> item = new HashMap<String, String>(origItem);

						itemMap.put(item.get(XDC_EXT_TOKEN_id), item);
//						cnt.add("substitutionGroup - " + sg);

						for (String k : item.keySet()) {
							cntCols.add(k);
						}

						Map<String, String> labels = mc.getRes("en", item);
						for (Map.Entry<String, String> li : labels.entrySet()) {
							String k = li.getKey();
							cntLabels.add(k);
							item.put(k, li.getValue());
						}
					}
					break;
				}
				return null;
			}
		});

		XbrlDock.log(EventLevel.Info, cntCols);
		XbrlDock.log(EventLevel.Info, cntLabels);

		try (PrintStream exp = new PrintStream("work/temp/EsgItems.csv")) {
			StringBuilder sb = null;
			for (Object k : cntCols.keys()) {
				sb = XbrlDockUtils.sbAppend(sb, "\t", true, k);
			}
			for (Object k : cntLabels.keys()) {
				sb = XbrlDockUtils.sbAppend(sb, "\t", true, k);
			}

			exp.println(sb);
			exp.flush();

			for (Map<String, String> item : itemMap.values()) {
				sb = null;
				for (Object k : cntCols.keys()) {
					sb = XbrlDockUtils.sbAppend(sb, "\t", true, item.get(k));
				}
				for (Object k : cntLabels.keys()) {
					sb = XbrlDockUtils.sbAppend(sb, "\t", true, item.get(k));
				}
				exp.println(sb);
				exp.flush();
			}
		} catch (Throwable e) {
			XbrlDockException.swallow(e);
		}

	}
}
