package hu.sze.milab.dustsimple;

public interface DustConsts {
	String EXT_JAR = ".jar";
	String EXT_JSON = ".json";
	String SEP = "_";

	int KEY_ADD = -1;
	int KEY_SIZE = -2;
	int KEY_ITER = -3;
	int KEY_KEYS = -4;

	interface MindHandle {};

	interface MindAgent {
		MindStatus agentExecAction(MindAction action) throws Exception;
	}

	enum MindValType {
		tagValtypeInt, tagValtypeReal, tagValtypeHandle, tagValtypeBin;
	};

	enum MindColl {
		One, Set, Arr, Map;
	};

	enum BootParam {
		LaunchParams, LaunchConfig, AppModule
	};

	enum MindRoot {
		Action, Self, Dialog,
	};

	enum MindAccess {
		Check, Peek, Commit, Get, Set, Insert, Delete,
	};

	enum MindAction {
		Init, Begin, Process, End, Release,
	};

	enum MindStatus {
		Waiting, Processing, Reject, Pass, Read, ReadAccept, Accept, Error,
	};

	enum MindListener {
		Interceptor, Monitor, Processor, Follower,
	};
}
