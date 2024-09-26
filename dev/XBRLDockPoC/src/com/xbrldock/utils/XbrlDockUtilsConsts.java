package com.xbrldock.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;

@SuppressWarnings({ "rawtypes", "unchecked" })
public interface XbrlDockUtilsConsts extends XbrlDockConsts {
	
	class ItemCreatorSimple<Type> implements ItemCreator<Type> {

		Class cc;

		public ItemCreatorSimple(Class cc) {
			this.cc = cc;
		}

		@Override
		public Type create(Object key, Object... hints) {
			try {
				return (Type) cc.getDeclaredConstructor().newInstance();
			} catch (Throwable e) {
				return XbrlDockException.wrap(e, key, hints);
			}
		}
	}

	ItemCreator<Map> MAP_CREATOR = new ItemCreatorSimple<>(HashMap.class);
	ItemCreator<Set> SET_CREATOR = new ItemCreatorSimple<>(HashSet.class);
	ItemCreator<ArrayList> ARRAY_CREATOR = new ItemCreatorSimple<>(ArrayList.class);

}
