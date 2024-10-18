package com.xbrldock.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.xbrldock.XbrlDockConsts;
import com.xbrldock.XbrlDockException;

@SuppressWarnings({ "rawtypes", "unchecked" })
public interface XbrlDockUtilsConsts extends XbrlDockConsts {

	class CounterProcessor<ItemType> implements GenProcessor<ItemType> {
		long begin;
		long process;
		long end;

		@Override
		public boolean process(ProcessorAction action, ItemType item) throws Exception {
			switch (action) {
			case Begin:
				++begin;
				break;
			case End:
				++end;
				break;
			case Init:
				begin = process = end = 0;
				break;
			case Process:
				++process;
				break;
			case Release:
				break;
			}
			return true;
		}

		public long getCount() {
			return process;
		}

		public long getDepth() {
			return begin - end;
		}

		public long getCount(ProcessorAction action) {
			switch (action) {
			case Begin:
				return begin;
			case End:
				return end;
			case Process:
				return process;
			default:
				return 0;
			}
		}

	}

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

	interface ObjectFormatter<ObType> {
		public String toString(ObType value, Object root, Object... hints);
	}

	ObjectFormatter<Collection> FMT_COLL = new ObjectFormatter<Collection>() {
		@Override
		public String toString(Collection value, Object root, Object... hints) {
			int l = (null == value) ? 0 : value.size();
			return "[ " + l + " ]";
		}
	};

	ObjectFormatter<Map> FMT_MAP = new ObjectFormatter<Map>() {
		@Override
		public String toString(Map value, Object root, Object... hints) {
			int l = (null == value) ? 0 : value.size();
			return "{ " + l + " }";
		}
	};

	ObjectFormatter<Object> FMT_TOHTML = new ObjectFormatter<Object>() {
		@Override
		public String toString(Object value, Object root, Object... hints) {
			return (null == value) ? "" : append(new StringBuilder(), "", value).toString();
		}

		StringBuilder append(StringBuilder sb, String indent, Object ob) {
			sb.append(indent);

			if (ob instanceof Map) {
				Map<String, Object> map = (Map) ob;

				if (!map.isEmpty()) {
					sb.append("<ul> \n");

					String i = indent + "  ";
					for (Map.Entry<String, Object> e : map.entrySet()) {
						sb.append(i).append("<li><b>").append(XbrlDockUtils.camel2Label(e.getKey())).append(":</b>  ");
						append(sb, i, e.getValue());
						sb.append(i).append("</li>\n");
					}
					sb.append(indent).append("</ul>");
				}
			} else if (ob instanceof Collection) {
				Collection coll = (Collection) ob;

				if (!coll.isEmpty()) {
					sb.append("<ul> \n");
					String i = indent + "  ";
					for (Object c : coll) {
						sb.append(i).append("<li>");
						append(sb, i, c);
						sb.append(i).append("</li>\n");
					}
					sb.append(indent).append("</ul>");
				}
			} else if (null != ob) {
				sb.append(XbrlDockUtils.toString(ob));
			}

			return sb;
		}
	};

	class LabeledAccess {
		public final String label;
		private final Object[] path;

		private final Object defVal;
		private final ObjectFormatter fmt;

		public LabeledAccess(String l, Object d, Object... p) {
			this.label = l;
			this.path = p;
			this.defVal = d;
			this.fmt = null;
		}

		public LabeledAccess(String l, ObjectFormatter f, Object... p) {
			this.label = l;
			this.path = p;
			this.defVal = null;
			this.fmt = f;
		}

		public <RetType> RetType get(Object from) {
			Object ret = XbrlDockUtils.simpleGet(from, path);
			return (RetType) ((null == fmt) ? (null == ret) ? defVal : ret : fmt.toString(ret, from, path));
		}
	}

	ItemCreator<Map> MAP_CREATOR = new ItemCreatorSimple<>(HashMap.class);
	ItemCreator<Map> SORTEDMAP_CREATOR = new ItemCreatorSimple<>(TreeMap.class);
	ItemCreator<Set> SET_CREATOR = new ItemCreatorSimple<>(HashSet.class);
	ItemCreator<Set> SORTEDSET_CREATOR = new ItemCreatorSimple<>(TreeSet.class);
	ItemCreator<ArrayList> ARRAY_CREATOR = new ItemCreatorSimple<>(ArrayList.class);

}
