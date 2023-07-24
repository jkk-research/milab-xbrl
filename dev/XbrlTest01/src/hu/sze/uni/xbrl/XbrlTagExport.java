package hu.sze.uni.xbrl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class XbrlTagExport implements XbrlConsts, XbrlListener {

	Map filing;
	Map<String, Map> contexts = new TreeMap<>();
	Map<String, Map> units = new TreeMap<>();

	@Override
	public void handleDocInfo(Map info) {
	}

	void setCurrentFiling(Map filing, File fRep) {
		this.filing = filing;
		System.out.println("Reading file: " + fRep.getAbsolutePath());
	}

	@Override
	public void handleXbrlInfo(XbrlInfoType type, Map info) {
		switch ( type ) {
		case Context:
			contexts.put((String) info.get("id"), new HashMap(info));
			break;
		case Unit:
			units.put((String) info.get("id"), new HashMap(info));
			break;
		case Fact:
			String tag = XbrlUtils.access(info, AccessCmd.Peek, null, "dimensions", "concept");
			if ( null != tag ) {
				int sep = tag.indexOf(':');
				String ns = tag.substring(0, sep);
				String t = tag.substring(sep + 1);
			}

			break;
		}
	}

	@Override
	public void dump() {
		// TODO Auto-generated method stub
		
	}

}
