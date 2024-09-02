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

	interface DustCreator<Type> {
		Type create(Object key, Object... hints);
	}

	class DustCreatorSimple<Type> implements DustCreator<Type> {

		Class cc;

		public DustCreatorSimple(Class cc) {
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

	DustCreator<Map> MAP_CREATOR = new DustCreatorSimple<>(HashMap.class);
	DustCreator<Set> SET_CREATOR = new DustCreatorSimple<>(HashSet.class);
	DustCreator<ArrayList> ARRAY_CREATOR = new DustCreatorSimple<>(ArrayList.class);

}
