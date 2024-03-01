package hu.sze.milab.dust;

public interface DustMetaConsts extends DustConsts {
	// Generated: 2024-03-01T14_30_36

	MindHandle APP_UNIT = Dust.lookup("giskard.me:0");

	MindHandle APP_MACHINE_MAIN = Dust.lookup("giskard.me:0:0");

	MindHandle APP_MODULE_MAIN = Dust.lookup("giskard.me:0:1");

	MindHandle APP_ASSEMBLY_MAIN = Dust.lookup("giskard.me:0:2");

	MindHandle MIND_UNIT = Dust.lookup("giskard.me:1");

	MindHandle MIND_ASP_UNIT = Dust.lookup("giskard.me:1:0");
	MindHandle MIND_ATT_UNIT_HANDLES = Dust.lookup("giskard.me:1:1");
	MindHandle MIND_ATT_UNIT_CONTENT = Dust.lookup("giskard.me:1:2");
	MindHandle MIND_ATT_UNIT_AUTHOR = Dust.lookup("giskard.me:1:3");

	MindHandle MIND_ASP_HANDLE = Dust.lookup("giskard.me:1:4");
	MindHandle MIND_ATT_HANDLE_UNIT = Dust.lookup("giskard.me:1:5");
	MindHandle MIND_ATT_HANDLE_ID = Dust.lookup("giskard.me:1:6");

	MindHandle MIND_ASP_KNOWLEDGE = Dust.lookup("giskard.me:1:7");
	MindHandle MIND_ATT_KNOWLEDGE_HANDLE = Dust.lookup("giskard.me:1:8");
	MindHandle MIND_ATT_KNOWLEDGE_TAGS = Dust.lookup("giskard.me:1:9");
	MindHandle MIND_ATT_KNOWLEDGE_LISTENERS = Dust.lookup("giskard.me:1:10");
	MindHandle MIND_ATT_KNOWLEDGE_PRIMARYASPECT = Dust.lookup("giskard.me:1:11");

	MindHandle MIND_ASP_ASPECT = Dust.lookup("giskard.me:1:12");
	MindHandle MIND_ASP_ATTRIBUTE = Dust.lookup("giskard.me:1:13");
	MindHandle MIND_ASP_TAG = Dust.lookup("giskard.me:1:14");
	MindHandle MIND_ASP_LOGIC = Dust.lookup("giskard.me:1:15");
	MindHandle MIND_ASP_ASSEMBLY = Dust.lookup("giskard.me:1:16");
	MindHandle MIND_ATT_ASSEMBLY_UNITS = Dust.lookup("giskard.me:1:17");
	MindHandle MIND_ATT_ASSEMBLY_STARTAGENTS = Dust.lookup("giskard.me:1:18");

	MindHandle MIND_ASP_DIALOG = Dust.lookup("giskard.me:1:19");
	MindHandle MIND_ATT_DIALOG_ASSEMBLY = Dust.lookup("giskard.me:1:20");
	MindHandle MIND_ATT_DIALOG_LAUNCHPARAMS = Dust.lookup("giskard.me:1:21");
	MindHandle MIND_ATT_DIALOG_ACTIVEAGENT = Dust.lookup("giskard.me:1:22");

	MindHandle MIND_ASP_AGENT = Dust.lookup("giskard.me:1:23");
	MindHandle MIND_ATT_AGENT_LOGIC = Dust.lookup("giskard.me:1:24");
	MindHandle MIND_ATT_AGENT_TARGET = Dust.lookup("giskard.me:1:25");

	MindHandle MIND_TAG_CONTEXT = Dust.lookup("giskard.me:1:26");
	MindHandle MIND_TAG_CONTEXT_DIALOG = Dust.lookup("giskard.me:1:27");
	MindHandle MIND_TAG_CONTEXT_SELF = Dust.lookup("giskard.me:1:28");
	MindHandle MIND_TAG_CONTEXT_TARGET = Dust.lookup("giskard.me:1:29");
	MindHandle MIND_TAG_CONTEXT_DIRECT = Dust.lookup("giskard.me:1:30");

	MindHandle MIND_TAG_VALTYPE = Dust.lookup("giskard.me:1:31");
	MindHandle MIND_TAG_VALTYPE_INT = Dust.lookup("giskard.me:1:32");
	MindHandle MIND_TAG_VALTYPE_REAL = Dust.lookup("giskard.me:1:33");
	MindHandle MIND_TAG_VALTYPE_HANDLE = Dust.lookup("giskard.me:1:34");
	MindHandle MIND_TAG_VALTYPE_BIN = Dust.lookup("giskard.me:1:35");

	MindHandle MIND_TAG_COLLTYPE = Dust.lookup("giskard.me:1:36");
	MindHandle MIND_TAG_COLLTYPE_ONE = Dust.lookup("giskard.me:1:37");
	MindHandle MIND_TAG_COLLTYPE_SET = Dust.lookup("giskard.me:1:38");
	MindHandle MIND_TAG_COLLTYPE_ARR = Dust.lookup("giskard.me:1:39");
	MindHandle MIND_TAG_COLLTYPE_MAP = Dust.lookup("giskard.me:1:40");

	MindHandle MIND_TAG_ACCESS = Dust.lookup("giskard.me:1:41");
	MindHandle MIND_TAG_ACCESS_CHECK = Dust.lookup("giskard.me:1:42");
	MindHandle MIND_TAG_ACCESS_PEEK = Dust.lookup("giskard.me:1:43");
	MindHandle MIND_TAG_ACCESS_GET = Dust.lookup("giskard.me:1:44");
	MindHandle MIND_TAG_ACCESS_SET = Dust.lookup("giskard.me:1:45");
	MindHandle MIND_TAG_ACCESS_INSERT = Dust.lookup("giskard.me:1:46");
	MindHandle MIND_TAG_ACCESS_DELETE = Dust.lookup("giskard.me:1:47");
	MindHandle MIND_TAG_ACCESS_RESET = Dust.lookup("giskard.me:1:48");
	MindHandle MIND_TAG_ACCESS_COMMIT = Dust.lookup("giskard.me:1:49");
	MindHandle MIND_TAG_ACCESS_BROADCAST = Dust.lookup("giskard.me:1:50");
	MindHandle MIND_TAG_ACCESS_LOOKUP = Dust.lookup("giskard.me:1:51");

	MindHandle MIND_TAG_ACTION = Dust.lookup("giskard.me:1:52");
	MindHandle MIND_TAG_ACTION_INIT = Dust.lookup("giskard.me:1:53");
	MindHandle MIND_TAG_ACTION_BEGIN = Dust.lookup("giskard.me:1:54");
	MindHandle MIND_TAG_ACTION_PROCESS = Dust.lookup("giskard.me:1:55");
	MindHandle MIND_TAG_ACTION_END = Dust.lookup("giskard.me:1:56");
	MindHandle MIND_TAG_ACTION_RELEASE = Dust.lookup("giskard.me:1:57");

	MindHandle MIND_TAG_STATUS = Dust.lookup("giskard.me:1:58");
	MindHandle MIND_TAG_STATUS_IDLE = Dust.lookup("giskard.me:1:59");
	MindHandle MIND_TAG_STATUS_PROCESSING = Dust.lookup("giskard.me:1:60");
	MindHandle MIND_TAG_STATUS_WAITING = Dust.lookup("giskard.me:1:61");
	MindHandle MIND_TAG_STATUS_ERROR = Dust.lookup("giskard.me:1:62");

	MindHandle MIND_TAG_RESULT = Dust.lookup("giskard.me:1:63");
	MindHandle MIND_TAG_RESULT_REJECT = Dust.lookup("giskard.me:1:64");
	MindHandle MIND_TAG_RESULT_PASS = Dust.lookup("giskard.me:1:65");
	MindHandle MIND_TAG_RESULT_READ = Dust.lookup("giskard.me:1:66");
	MindHandle MIND_TAG_RESULT_READACCEPT = Dust.lookup("giskard.me:1:67");
	MindHandle MIND_TAG_RESULT_ACCEPT = Dust.lookup("giskard.me:1:68");

	MindHandle MIND_ASP_AUTHOR = Dust.lookup("giskard.me:1:69");

	MindHandle DUST_UNIT = Dust.lookup("giskard.me:2");

	MindHandle DUST_ASP_MACHINE = Dust.lookup("giskard.me:2:0");
	MindHandle DUST_ATT_MACHINE_AUTHORS = Dust.lookup("giskard.me:2:1");
	MindHandle DUST_ATT_MACHINE_UNITS = Dust.lookup("giskard.me:2:2");
	MindHandle DUST_ATT_MACHINE_ASSEMBLIES = Dust.lookup("giskard.me:2:3");
	MindHandle DUST_ATT_MACHINE_MAINASSEMBLY = Dust.lookup("giskard.me:2:4");
	MindHandle DUST_ATT_MACHINE_MODULES = Dust.lookup("giskard.me:2:5");
	MindHandle DUST_ATT_MACHINE_DIALOGS = Dust.lookup("giskard.me:2:6");
	MindHandle DUST_ATT_MACHINE_MAINDIALOG = Dust.lookup("giskard.me:2:7");
	MindHandle DUST_ATT_MACHINE_THREADS = Dust.lookup("giskard.me:2:8");
	MindHandle DUST_ATT_MACHINE_ALL_IMPLEMENTATIONS = Dust.lookup("giskard.me:2:9");
	MindHandle DUST_ATT_MACHINE_ACTIVE_SERVERS = Dust.lookup("giskard.me:2:10");

	MindHandle DUST_ASP_MODULE = Dust.lookup("giskard.me:2:11");
	MindHandle DUST_ATT_MODULE_UNITS = Dust.lookup("giskard.me:2:12");
	MindHandle DUST_ATT_MODULE_NATIVELOGICS = Dust.lookup("giskard.me:2:13");
	MindHandle DUST_ATT_MODULE_LIBRARIES = Dust.lookup("giskard.me:2:14");

	MindHandle DUST_ASP_THREAD = Dust.lookup("giskard.me:2:15");
	MindHandle DUST_ATT_THREAD_DIALOG = Dust.lookup("giskard.me:2:16");

	MindHandle DUST_ASP_NATIVELOGIC = Dust.lookup("giskard.me:2:17");
	MindHandle DUST_ATT_NATIVELOGIC_LOGIC = Dust.lookup("giskard.me:2:18");
	MindHandle DUST_ATT_NATIVELOGIC_IMPLEMENTATION = Dust.lookup("giskard.me:2:19");
	MindHandle DUST_ATT_NATIVELOGIC_INSTANCE = Dust.lookup("giskard.me:2:20");

	MindHandle DUST_TAG_NATIVELOGIC_SERVER = Dust.lookup("giskard.me:2:21");

	MindHandle MISC_UNIT = Dust.lookup("giskard.me:3");

	MindHandle MISC_ASP_CONN = Dust.lookup("giskard.me:3:0");
	MindHandle MISC_ATT_CONN_OWNER = Dust.lookup("giskard.me:3:1");
	MindHandle MISC_ATT_CONN_PARENT = Dust.lookup("giskard.me:3:2");
	MindHandle MISC_ATT_CONN_SOURCE = Dust.lookup("giskard.me:3:3");
	MindHandle MISC_ATT_CONN_TARGET = Dust.lookup("giskard.me:3:4");
	MindHandle MISC_ATT_CONN_REQUIRES = Dust.lookup("giskard.me:3:5");
	MindHandle MISC_ATT_CONN_MEMBERMAP = Dust.lookup("giskard.me:3:6");
	MindHandle MISC_ATT_CONN_MEMBERARR = Dust.lookup("giskard.me:3:7");
	MindHandle MISC_ATT_CONN_MEMBERSET = Dust.lookup("giskard.me:3:8");

	MindHandle MISC_ASP_ALIAS = Dust.lookup("giskard.me:3:9");
	MindHandle MISC_ASP_GEN = Dust.lookup("giskard.me:3:10");
	MindHandle MISC_ATT_GEN_COUNT = Dust.lookup("giskard.me:3:11");

	MindHandle MISC_TAG_EMPTY = Dust.lookup("giskard.me:3:12");

	MindHandle MISC_TAG_ACTIVE = Dust.lookup("giskard.me:3:13");

	MindHandle MISC_TAG_DIRECTION = Dust.lookup("giskard.me:3:14");
	MindHandle MISC_TAG_DIRECTION_IN = Dust.lookup("giskard.me:3:15");
	MindHandle MISC_TAG_DIRECTION_OUT = Dust.lookup("giskard.me:3:16");

	MindHandle MISC_ASP_VARIANT = Dust.lookup("giskard.me:3:17");
	MindHandle MISC_ATT_VARIANT_VALUE = Dust.lookup("giskard.me:3:18");

	MindHandle TEXT_UNIT = Dust.lookup("giskard.me:4");

	MindHandle TEXT_TAG_LANGUAGE = Dust.lookup("giskard.me:4:0");
	MindHandle TEXT_TAG_LANGUAGE_EN_US = Dust.lookup("giskard.me:4:1");

	MindHandle TEXT_ATT_LANGUAGE_DEFAULT = Dust.lookup("giskard.me:4:2");
	MindHandle TEXT_ATT_TOKEN = Dust.lookup("giskard.me:4:3");

	MindHandle TEXT_TAG_TYPE = Dust.lookup("giskard.me:4:4");
	MindHandle TEXT_TAG_TYPE_TOKEN = Dust.lookup("giskard.me:4:5");
	MindHandle TEXT_TAG_TYPE_NAME = Dust.lookup("giskard.me:4:6");

	MindHandle TEXT_ASP_PLAIN = Dust.lookup("giskard.me:4:7");
	MindHandle TEXT_ATT_PLAIN_TEXT = Dust.lookup("giskard.me:4:8");

	MindHandle EVENT_UNIT = Dust.lookup("giskard.me:5");

	MindHandle EVENT_ASP_EVENT = Dust.lookup("giskard.me:5:0");

	MindHandle EVENT_TAG_TYPE = Dust.lookup("giskard.me:5:1");
	MindHandle EVENT_TAG_TYPE_EXCEPTIONTHROWN = Dust.lookup("giskard.me:5:2");
	MindHandle EVENT_TAG_TYPE_EXCEPTIONSWALLOWED = Dust.lookup("giskard.me:5:3");
	MindHandle EVENT_TAG_TYPE_ERROR = Dust.lookup("giskard.me:5:4");
	MindHandle EVENT_TAG_TYPE_WARNING = Dust.lookup("giskard.me:5:5");
	MindHandle EVENT_TAG_TYPE_INFO = Dust.lookup("giskard.me:5:6");
	MindHandle EVENT_TAG_TYPE_TRACE = Dust.lookup("giskard.me:5:7");
	MindHandle EVENT_TAG_TYPE_BREAKPOINT = Dust.lookup("giskard.me:5:8");

	MindHandle RESOURCE_UNIT = Dust.lookup("giskard.me:6");

	MindHandle RESOURCE_ASP_URL = Dust.lookup("giskard.me:6:0");
	MindHandle RESOURCE_ATT_URL_SCHEME = Dust.lookup("giskard.me:6:1");
	MindHandle RESOURCE_ATT_URL_USERINFO = Dust.lookup("giskard.me:6:2");
	MindHandle RESOURCE_ATT_URL_HOST = Dust.lookup("giskard.me:6:3");
	MindHandle RESOURCE_ATT_URL_PATH = Dust.lookup("giskard.me:6:4");
	MindHandle RESOURCE_ATT_URL_QUERY = Dust.lookup("giskard.me:6:5");
	MindHandle RESOURCE_ATT_URL_FRAGMENT = Dust.lookup("giskard.me:6:6");

	MindHandle RESOURCE_ASP_STREAM = Dust.lookup("giskard.me:6:7");

	MindHandle RESOURCE_TAG_STREAMTYPE = Dust.lookup("giskard.me:6:8");
	MindHandle RESOURCE_TAG_STREAMTYPE_RAW = Dust.lookup("giskard.me:6:9");
	MindHandle RESOURCE_TAG_STREAMTYPE_TEXT = Dust.lookup("giskard.me:6:10");
	MindHandle RESOURCE_TAG_STREAMTYPE_ZIPENTRY = Dust.lookup("giskard.me:6:11");

	MindHandle RESOURCE_ASP_PROCESSOR = Dust.lookup("giskard.me:6:12");
	MindHandle RESOURCE_ATT_PROCESSOR_STREAM = Dust.lookup("giskard.me:6:13");
	MindHandle RESOURCE_ATT_PROCESSOR_DATA = Dust.lookup("giskard.me:6:14");

	MindHandle RESOURCE_AGT_FILESYSTEM = Dust.lookup("giskard.me:6:15");
	MindHandle RESOURCE_AGT_ZIPREADER = Dust.lookup("giskard.me:6:16");
	MindHandle RESOURCE_AGT_JSONDOM = Dust.lookup("giskard.me:6:17");
	MindHandle RESOURCE_AGT_CSVSAX = Dust.lookup("giskard.me:6:18");
	MindHandle RESOURCE_ATT_CSVSAX_SEP = Dust.lookup("giskard.me:6:19");

	MindHandle DEV_UNIT = Dust.lookup("giskard.me:7");

	MindHandle DEV_ATT_HINT = Dust.lookup("giskard.me:7:0");

	MindHandle DUSTJAVA_UNIT = Dust.lookup("giskard.me:8");

}