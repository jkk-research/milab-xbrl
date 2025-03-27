package org.xbrldock.vsme.poc;

import java.awt.Dimension;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Icon;

import com.xbrldock.poc.gui.XbrlDockGuiUtils;
import com.xbrldock.utils.XbrlDockUtils;

public class VsmeUtils implements VsmePocConsts {
	private static Dimension ICON_DIM = new Dimension(15, 15);
	private static Map<String, Icon> ICON_MAP = new TreeMap<>();
	private static ItemCreator<Icon> ICON_CREATOR = new ItemCreator<Icon>() {
		@Override
		public Icon create(Object key, Object... hints) {
			return XbrlDockGuiUtils.getIcon((String) key, ICON_DIM);
		}
	};

	public static Icon getCmdIcon(String cmd) {
		return XbrlDockUtils.safeGet(ICON_MAP, cmd, ICON_CREATOR);
	}
}
