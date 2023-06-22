package hu.sze.milab.dust.brain;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import hu.sze.milab.dust.Dust;
import hu.sze.milab.dust.DustMetaConsts;
import hu.sze.milab.dust.stream.DustStreamConsts;
import hu.sze.milab.dust.utils.DustUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class DustBrain implements DustBrainConsts, DustImpl.BrainImpl {

	static Map<MindHandle, Map> tempKnowledge = new HashMap<>();

	MindHandle hUnit = MIND_ATT_KNOWLEDGE_UNIT;

	public DustBrain() {
		loadConstsFrom(DustMetaConsts.class);
		loadConstsFrom(DustStreamConsts.class);
	}

	public void loadConstsFrom(Class constClass) {
		Map<DustBrainHandle, String> handleToName = new HashMap<>();
		Map<String, DustBrainHandle> nameToHandle = new HashMap<>();

		for (Field f : constClass.getDeclaredFields()) {
			try {
				Object bh = f.get(null);
				if ( bh instanceof MindHandle ) {
					handleToName.put((DustBrainHandle) bh, f.getName());
					nameToHandle.put(f.getName(), (DustBrainHandle) bh);
				}
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for (Map.Entry<DustBrainHandle, String> ke : handleToName.entrySet()) {
			DustBrainHandle bh = ke.getKey();
			String name = ke.getValue();
			Map k = resolveKnowledge(bh, true);

			k.put(TEXT_ATT_NAMED_NAME, name);

			String[] nameParts = name.split("_");

			String unitName = nameParts[0] + "_" + "UNIT";

			DustBrainHandle hTarget = nameToHandle.get(unitName);
			k.put(MIND_ATT_KNOWLEDGE_UNIT, hTarget);
		}
	}

	static public String handleToString(DustBrainHandle bh) {
		Map k = resolveKnowledge(bh, false);
		return (null == k) ? "???" : (String) k.getOrDefault(DustMetaConsts.TEXT_ATT_NAMED_NAME, "???");
	}

	static public void dumpHandle(String prefix, MindHandle bh) {
		Map k = resolveKnowledge(bh, false);
		Dust.dump(prefix, (null == k) ? "???" : DustUtils.toString(k));
	}

	static public Map resolveKnowledge(MindHandle bh, boolean createIfMissing) {
		if ( null == bh ) {
			bh = new DustBrainHandle();
			createIfMissing = true;
		}

		synchronized (bh) {
			Map k = tempKnowledge.get(bh);

			if ( (null == k) && createIfMissing ) {
				k = new HashMap<>();
				k.put(MIND_ATT_KNOWLEDGE_HANDLE, bh);
				tempKnowledge.put(bh, k);
			}

			return k;
		}
	}

	@Override
	public MindHandle createHandle() {
		return new DustBrainHandle();
	}

	public <RetType> RetType access(Object root, MindAccess cmd, Object val, Object... path) {
		Object ret = null;

		Object curr = root;
		Object prev = null;
		Object lastKey = null;

		for (Object p : path) {
			if ( curr instanceof MindHandle ) {
				curr = resolveKnowledge((DustBrainHandle) curr, true);
			}
			prev = curr;
			lastKey = p;

			if ( curr instanceof ArrayList ) {
				curr = ((ArrayList) curr).get((Integer) p);
			} else if ( curr instanceof Map ) {
				curr = ((Map) curr).get(p);
			} else {
				curr = null;
			}

			if ( null == curr ) {
				break;
			}
		}

		ret = (null == curr) ? val : curr;

		switch ( cmd ) {
		case Check:
			break;
		case Commit:

			if ( curr instanceof MindHandle ) {
				MindAgent a = Dust.access(curr, MindAccess.Peek, null, MIND_ATT_KNOWLEDGE_LISTENERS, BRAIN_ATT_ACTOR_INSTANCE);

				if ( null != a ) {
					try {
						a.agentExecAction((MindAction) val);
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			}
			break;
		case Delete:
			break;
		case Get:
			break;
		case Insert:
			break;
		case Peek:
			break;
		case Reset:
			if ( curr instanceof MindHandle ) {
				curr = resolveKnowledge((DustBrainHandle) curr, false);
			}
			if ( curr instanceof Map ) {
				((Map) curr).clear();
			}
			break;
		case Set:
			if ( (null != lastKey) && (prev instanceof Map) ) {
				((Map) prev).put(lastKey, val);
			}
			break;
		default:
			break;
		}

		return (RetType) ret;
	}

}
