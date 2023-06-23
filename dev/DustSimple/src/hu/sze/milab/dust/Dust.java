package hu.sze.milab.dust;

import hu.sze.milab.dust.utils.DustUtils;

public class Dust implements DustConsts {
	
	protected interface Brain {
		MindHandle createHandle();
		<RetType> RetType access(Object root, MindAccess cmd, Object val, Object... path);
	}
	
	protected static Brain BRAIN;
	
	protected static void setBrain(Brain brain) {
		BRAIN = brain;
	}
	
	public static <RetType> RetType access(Object root, MindAccess cmd, Object val, Object... path) {
		return BRAIN.access(root,  cmd,  val, path);
	}

	public static MindHandle createHandle() {
		return BRAIN.createHandle();
	}

	public static void dump(Object sep, boolean strict, Object... objects) {
		StringBuilder sb = DustUtils.sbAppend(null, sep, strict, objects);
		
		if ( null != sb ) {
			System.out.println(sb);
		}
	}

	public static void dumpObs(Object... obs) {
		log(null, obs);
	}

	public static void log(Object event, Object... params) {
		dump(", ", false, params);
	}
}
