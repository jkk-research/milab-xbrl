package hu.sze.uni.xbrl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import hu.sze.uni.xbrl.XbrlConsts.XbrlInfoType;

@SuppressWarnings("rawtypes")
public interface XbrlListener {
	void handleDocInfo(Map info);

	void handleXbrlInfo(XbrlInfoType type, Map info);

	void dump();

	public class Dumper implements XbrlListener {
		Collection<String> keys;

		public Dumper(String... kk) {
			keys = Arrays.asList(kk);
		}

		@Override
		public void handleDocInfo(Map info) {
			System.out.println("Document info " + info);
		}

		@Override
		public void handleXbrlInfo(XbrlInfoType type, Map info) {
			switch ( type ) {
			case Fact: {
				Map dim = (Map) info.get("dimensions");

				if ( null != dim ) {
					String key = ((String) dim.get("concept")).toLowerCase();
					boolean match = false;

					for (String k : keys) {
						if ( key.contains(k.toLowerCase()) ) {
							match = true;
							break;
						}
					}

					if ( match ) {
						System.out.println(dim.get("concept") + " [" + dim.get("period") + "] = " + info.get("value") + " " + dim.get("unit"));
					}
				}
			}
				break;
			default:
				break;
			}
		}

		public void dump() {
		};
	}

	public class Collector implements XbrlListener {
		XbrlUtilsCounter concepts = new XbrlUtilsCounter(true);

		@Override
		public void handleDocInfo(Map info) {
			System.out.println("Document info " + info);
		}

		@Override
		public void handleXbrlInfo(XbrlInfoType type, Map info) {
			switch ( type ) {
			case Fact: {
				Map dim = (Map) info.get("dimensions");
				if ( null != dim ) {
					concepts.add((String) dim.get("concept"));
				}
			}
				break;
			default:
				System.out.println(type + " " + info);
				break;
			}
		}

		public void dump() {
			for (Map.Entry<Object, Long> c : concepts) {
				System.out.println(c.getKey() + ": " + c.getValue());
			}
		}
	}
}