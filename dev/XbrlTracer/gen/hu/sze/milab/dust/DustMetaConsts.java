package hu.sze.milab.dust;

public interface DustMetaConsts extends DustConsts {

	public static MindHandle APP_UNIT = Dust.recall("0");

	public static MindHandle APP_MACHINE_MAIN = Dust.recall("0:1");

	public static MindHandle APP_MODULE_MAIN = Dust.recall("0:2");

	public static MindHandle APP_ASSEMBLY_MAIN = Dust.recall("0:3");

	public static MindHandle MIND_UNIT = Dust.recall("1");

	public static MindHandle MIND_ASP_UNIT = Dust.recall("1:1");
	public static MindHandle MIND_ATT_UNIT_HANDLES = Dust.recall("1:2");
	public static MindHandle MIND_ATT_UNIT_AUTHOR = Dust.recall("1:3");

	public static MindHandle MIND_ASP_HANDLE = Dust.recall("1:4");
	public static MindHandle MIND_ATT_HANDLE_UNIT = Dust.recall("1:5");
	public static MindHandle MIND_ATT_HANDLE_ID = Dust.recall("1:6");

	public static MindHandle MIND_ASP_KNOWLEDGE = Dust.recall("1:7");
	public static MindHandle MIND_ATT_KNOWLEDGE_HANDLE = Dust.recall("1:8");
	public static MindHandle MIND_ATT_KNOWLEDGE_TAGS = Dust.recall("1:9");
	public static MindHandle MIND_ATT_KNOWLEDGE_LISTENERS = Dust.recall("1:10");
	public static MindHandle MIND_ATT_KNOWLEDGE_PRIMARYASPECT = Dust.recall("1:11");

	public static MindHandle MIND_ASP_ASPECT = Dust.recall("1:12");
	public static MindHandle MIND_ASP_ATTRIBUTE = Dust.recall("1:13");
	public static MindHandle MIND_ASP_TAG = Dust.recall("1:14");
	public static MindHandle MIND_ASP_LOGIC = Dust.recall("1:15");
	public static MindHandle MIND_ASP_ASSEMBLY = Dust.recall("1:16");
	public static MindHandle MIND_ATT_ASSEMBLY_UNITS = Dust.recall("1:17");
	public static MindHandle MIND_ATT_ASSEMBLY_STARTAGENTS = Dust.recall("1:18");

	public static MindHandle MIND_ASP_DIALOG = Dust.recall("1:19");
	public static MindHandle MIND_ATT_DIALOG_ASSEMBLY = Dust.recall("1:20");
	public static MindHandle MIND_ATT_DIALOG_KNOWLEDGE = Dust.recall("1:21");
	public static MindHandle MIND_ATT_DIALOG_LAUNCHPARAMS = Dust.recall("1:22");
	public static MindHandle MIND_ATT_DIALOG_ACTIVEAGENT = Dust.recall("1:23");

	public static MindHandle MIND_ASP_AGENT = Dust.recall("1:24");
	public static MindHandle MIND_ATT_AGENT_LOGIC = Dust.recall("1:25");
	public static MindHandle MIND_ATT_AGENT_TARGET = Dust.recall("1:26");

	public static MindHandle MIND_TAG_CONTEXT = Dust.recall("1:27");
	public static MindHandle MIND_TAG_CONTEXT_DIALOG = Dust.recall("1:28");
	public static MindHandle MIND_TAG_CONTEXT_SELF = Dust.recall("1:29");
	public static MindHandle MIND_TAG_CONTEXT_TARGET = Dust.recall("1:30");
	public static MindHandle MIND_TAG_CONTEXT_DIRECT = Dust.recall("1:31");

	public static MindHandle MIND_TAG_VALTYPE = Dust.recall("1:32");
	public static MindHandle MIND_TAG_VALTYPE_INT = Dust.recall("1:33");
	public static MindHandle MIND_TAG_VALTYPE_REAL = Dust.recall("1:34");
	public static MindHandle MIND_TAG_VALTYPE_HANDLE = Dust.recall("1:35");
	public static MindHandle MIND_TAG_VALTYPE_BIN = Dust.recall("1:36");

	public static MindHandle MIND_TAG_COLLTYPE = Dust.recall("1:37");
	public static MindHandle MIND_TAG_COLLTYPE_ONE = Dust.recall("1:38");
	public static MindHandle MIND_TAG_COLLTYPE_SET = Dust.recall("1:39");
	public static MindHandle MIND_TAG_COLLTYPE_ARR = Dust.recall("1:40");
	public static MindHandle MIND_TAG_COLLTYPE_MAP = Dust.recall("1:41");

	public static MindHandle MIND_TAG_ACCESS = Dust.recall("1:42");
	public static MindHandle MIND_TAG_ACCESS_CHECK = Dust.recall("1:43");
	public static MindHandle MIND_TAG_ACCESS_PEEK = Dust.recall("1:44");
	public static MindHandle MIND_TAG_ACCESS_GET = Dust.recall("1:45");
	public static MindHandle MIND_TAG_ACCESS_SET = Dust.recall("1:46");
	public static MindHandle MIND_TAG_ACCESS_INSERT = Dust.recall("1:47");
	public static MindHandle MIND_TAG_ACCESS_DELETE = Dust.recall("1:48");
	public static MindHandle MIND_TAG_ACCESS_RESET = Dust.recall("1:49");
	public static MindHandle MIND_TAG_ACCESS_COMMIT = Dust.recall("1:50");

	public static MindHandle MIND_TAG_ACTION = Dust.recall("1:51");
	public static MindHandle MIND_TAG_ACTION_INIT = Dust.recall("1:52");
	public static MindHandle MIND_TAG_ACTION_BEGIN = Dust.recall("1:53");
	public static MindHandle MIND_TAG_ACTION_PROCESS = Dust.recall("1:54");
	public static MindHandle MIND_TAG_ACTION_END = Dust.recall("1:55");
	public static MindHandle MIND_TAG_ACTION_RELEASE = Dust.recall("1:56");

	public static MindHandle MIND_TAG_STATUS = Dust.recall("1:57");
	public static MindHandle MIND_TAG_STATUS_IDLE = Dust.recall("1:58");
	public static MindHandle MIND_TAG_STATUS_PROCESSING = Dust.recall("1:59");
	public static MindHandle MIND_TAG_STATUS_WAITING = Dust.recall("1:60");
	public static MindHandle MIND_TAG_STATUS_ERROR = Dust.recall("1:61");

	public static MindHandle MIND_TAG_RESULT = Dust.recall("1:62");
	public static MindHandle MIND_TAG_RESULT_REJECT = Dust.recall("1:63");
	public static MindHandle MIND_TAG_RESULT_PASS = Dust.recall("1:64");
	public static MindHandle MIND_TAG_RESULT_READ = Dust.recall("1:65");
	public static MindHandle MIND_TAG_RESULT_READACCEPT = Dust.recall("1:66");
	public static MindHandle MIND_TAG_RESULT_ACCEPT = Dust.recall("1:67");

	public static MindHandle MIND_ASP_AUTHOR = Dust.recall("1:68");

	public static MindHandle MIND_AUTHOR_DUST = Dust.recall("1:69");

	public static MindHandle DUST_UNIT = Dust.recall("2");

	public static MindHandle DUST_ASP_MACHINE = Dust.recall("2:1");
	public static MindHandle DUST_ATT_MACHINE_UNITS = Dust.recall("2:2");
	public static MindHandle DUST_ATT_MACHINE_ASSEMBLIES = Dust.recall("2:3");
	public static MindHandle DUST_ATT_MACHINE_MAINASSEMBLY = Dust.recall("2:4");
	public static MindHandle DUST_ATT_MACHINE_MODULES = Dust.recall("2:5");
	public static MindHandle DUST_ATT_MACHINE_DIALOGS = Dust.recall("2:6");
	public static MindHandle DUST_ATT_MACHINE_MAINDIALOG = Dust.recall("2:7");
	public static MindHandle DUST_ATT_MACHINE_THREADS = Dust.recall("2:8");
	public static MindHandle DUST_ATT_MACHINE_ALL_IMPLEMENTATIONS = Dust.recall("2:9");
	public static MindHandle DUST_ATT_MACHINE_ACTIVE_SERVERS = Dust.recall("2:10");

	public static MindHandle DUST_ASP_MODULE = Dust.recall("2:11");
	public static MindHandle DUST_ATT_MODULE_UNITS = Dust.recall("2:12");
	public static MindHandle DUST_ATT_MODULE_NATIVELOGICS = Dust.recall("2:13");
	public static MindHandle DUST_ATT_MODULE_LIBRARIES = Dust.recall("2:14");

	public static MindHandle DUST_ASP_THREAD = Dust.recall("2:15");
	public static MindHandle DUST_ATT_THREAD_DIALOG = Dust.recall("2:16");

	public static MindHandle DUST_ASP_NATIVELOGIC = Dust.recall("2:17");
	public static MindHandle DUST_ATT_NATIVELOGIC_LOGIC = Dust.recall("2:18");
	public static MindHandle DUST_ATT_NATIVELOGIC_IMPLEMENTATION = Dust.recall("2:19");
	public static MindHandle DUST_ATT_NATIVELOGIC_INSTANCE = Dust.recall("2:20");

	public static MindHandle MISC_UNIT = Dust.recall("3");

	public static MindHandle MISC_ASP_CONN = Dust.recall("3:1");
	public static MindHandle MISC_ATT_CONN_OWNER = Dust.recall("3:2");
	public static MindHandle MISC_ATT_CONN_PARENT = Dust.recall("3:3");
	public static MindHandle MISC_ATT_CONN_SOURCE = Dust.recall("3:4");
	public static MindHandle MISC_ATT_CONN_TARGET = Dust.recall("3:5");
	public static MindHandle MISC_ATT_CONN_REQUIRES = Dust.recall("3:6");
	public static MindHandle MISC_ATT_CONN_MEMBERMAP = Dust.recall("3:7");
	public static MindHandle MISC_ATT_CONN_MEMBERARR = Dust.recall("3:8");
	public static MindHandle MISC_ATT_CONN_MEMBERSET = Dust.recall("3:9");

	public static MindHandle MISC_ASP_ALIAS = Dust.recall("3:10");
	public static MindHandle MISC_ASP_GEN = Dust.recall("3:11");
	public static MindHandle MISC_ATT_GEN_COUNT = Dust.recall("3:12");

	public static MindHandle MISC_TAG_EMPTY = Dust.recall("3:13");

	public static MindHandle MISC_TAG_ACTIVE = Dust.recall("3:14");

	public static MindHandle MISC_TAG_DIRECTION = Dust.recall("3:15");
	public static MindHandle MISC_TAG_DIRECTION_IN = Dust.recall("3:16");
	public static MindHandle MISC_TAG_DIRECTION_OUT = Dust.recall("3:17");

	public static MindHandle MISC_ASP_VARIANT = Dust.recall("3:18");
	public static MindHandle MISC_ATT_VARIANT_VALUE = Dust.recall("3:19");

	public static MindHandle TEXT_UNIT = Dust.recall("4");

	public static MindHandle TEXT_TAG_LANGUAGE = Dust.recall("4:1");
	public static MindHandle TEXT_TAG_LANGUAGE_EN_US = Dust.recall("4:2");

	public static MindHandle TEXT_ATT_LANGUAGE_DEFAULT = Dust.recall("4:3");
	public static MindHandle TEXT_ATT_TOKEN = Dust.recall("4:4");

	public static MindHandle TEXT_TAG_TYPE = Dust.recall("4:5");
	public static MindHandle TEXT_TAG_TYPE_TOKEN = Dust.recall("4:6");
	public static MindHandle TEXT_TAG_TYPE_NAME = Dust.recall("4:7");

	public static MindHandle TEXT_ASP_PLAIN = Dust.recall("4:8");
	public static MindHandle TEXT_ATT_PLAIN_TEXT = Dust.recall("4:9");

	public static MindHandle EVENT_UNIT = Dust.recall("5");

	public static MindHandle EVENT_ASP_EVENT = Dust.recall("5:1");

	public static MindHandle EVENT_TAG_TYPE = Dust.recall("5:2");
	public static MindHandle EVENT_TAG_TYPE_EXCEPTIONTHROWN = Dust.recall("5:3");
	public static MindHandle EVENT_TAG_TYPE_EXCEPTIONSWALLOWED = Dust.recall("5:4");
	public static MindHandle EVENT_TAG_TYPE_ERROR = Dust.recall("5:5");
	public static MindHandle EVENT_TAG_TYPE_WARNING = Dust.recall("5:6");
	public static MindHandle EVENT_TAG_TYPE_INFO = Dust.recall("5:7");
	public static MindHandle EVENT_TAG_TYPE_TRACE = Dust.recall("5:8");
	public static MindHandle EVENT_TAG_TYPE_BREAKPOINT = Dust.recall("5:9");

	public static MindHandle RESOURCE_UNIT = Dust.recall("6");

	public static MindHandle RESOURCE_ASP_URL = Dust.recall("6:1");
	public static MindHandle RESOURCE_ATT_URL_SCHEME = Dust.recall("6:2");
	public static MindHandle RESOURCE_ATT_URL_USERINFO = Dust.recall("6:3");
	public static MindHandle RESOURCE_ATT_URL_HOST = Dust.recall("6:4");
	public static MindHandle RESOURCE_ATT_URL_PATH = Dust.recall("6:5");
	public static MindHandle RESOURCE_ATT_URL_QUERY = Dust.recall("6:6");
	public static MindHandle RESOURCE_ATT_URL_FRAGMENT = Dust.recall("6:7");

	public static MindHandle RESOURCE_ASP_STREAM = Dust.recall("6:8");

	public static MindHandle RESOURCE_TAG_STREAMTYPE = Dust.recall("6:9");
	public static MindHandle RESOURCE_TAG_STREAMTYPE_RAW = Dust.recall("6:10");
	public static MindHandle RESOURCE_TAG_STREAMTYPE_TEXT = Dust.recall("6:11");
	public static MindHandle RESOURCE_TAG_STREAMTYPE_ZIPENTRY = Dust.recall("6:12");

	public static MindHandle RESOURCE_ASP_PROCESSOR = Dust.recall("6:13");
	public static MindHandle RESOURCE_ATT_PROCESSOR_STREAM = Dust.recall("6:14");
	public static MindHandle RESOURCE_ATT_PROCESSOR_DATA = Dust.recall("6:15");

	public static MindHandle RESOURCE_SRV_FILESYSTEM = Dust.recall("6:16");
	public static MindHandle RESOURCE_AGT_ZIPREADER = Dust.recall("6:17");
	public static MindHandle RESOURCE_AGT_JSONDOM = Dust.recall("6:18");
	public static MindHandle RESOURCE_AGT_CSVSAX = Dust.recall("6:19");

	public static MindHandle RESOURCE_ATT_CSVSAX_SEP = Dust.recall("6:20");

	public static MindHandle DEV_UNIT = Dust.recall("7");
	public static MindHandle DEV_ATT_HINT = Dust.recall("7:1");

	public static MindHandle DUSTJAVA_UNIT = Dust.recall("8");

	public static MindHandle L10N_UNIT = Dust.recall("9");

}