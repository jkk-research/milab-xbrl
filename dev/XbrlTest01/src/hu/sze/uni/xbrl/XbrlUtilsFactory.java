package hu.sze.uni.xbrl;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public abstract class XbrlUtilsFactory<KeyType, ValType> {
	String name;
	protected final Map<KeyType, ValType> content;

	public XbrlUtilsFactory(boolean sorted) {
		this.content = sorted ? new TreeMap<>() : new HashMap<>();
	}

	protected abstract ValType create(KeyType key, Object... hints);

	protected void initNew(ValType item, KeyType key, Object... hints) {

	}

	public synchronized ValType peek(KeyType key) {
		return content.get(key);
	}

	public synchronized ValType get(KeyType key, Object... hints) {
		ValType v = content.get(key);

		if (null == v) {
			v = create(key, hints);
			content.put(key, v);
			initNew(v, key, hints);
		}

		return v;
	}

	public synchronized void clear() {
		content.clear();
	}

	public Iterable<KeyType> keys() {
		return content.keySet();
	}

	public Iterable<ValType> values() {
		return content.values();
	}

	public void put(KeyType key, ValType value) {
		content.put(key, value);
	}

	public boolean drop(ValType value) {
		return content.values().remove(value);
	}

	public static class Simple<KeyType, ValType> extends XbrlUtilsFactory<KeyType, ValType> {
		private final Constructor<? extends ValType> constructor;

		public Simple(boolean sorted, Class<? extends ValType> clVal) {
			super(sorted);
			
			try {
				this.constructor = clVal.getConstructor();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		protected ValType create(KeyType key, Object... hints) {
			try {
				return constructor.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public int size() {
		return content.size();
	}
}
