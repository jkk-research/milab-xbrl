package hu.sze.milab.dust;

public interface DustMetaConsts extends DustConsts {

	public static MindHandle APP_UNIT = Dust.lookup("0");

	public static MindHandle APP_MACHINE_MAIN = Dust.lookup("0:1");

	public static MindHandle APP_MODULE_MAIN = Dust.lookup("0:2");

	public static MindHandle APP_ASSEMBLY_MAIN = Dust.lookup("0:3");

	public static MindHandle MIND_UNIT = Dust.lookup("1");

	public static MindHandle MIND_ASP_UNIT = Dust.lookup("1:1");
	public static MindHandle MIND_ATT_UNIT_HANDLES = Dust.lookup("1:2");
	public static MindHandle MIND_ATT_UNIT_AUTHOR = Dust.lookup("1:3");

	public static MindHandle MIND_ASP_HANDLE = Dust.lookup("1:4");
	public static MindHandle MIND_ATT_HANDLE_UNIT = Dust.lookup("1:5");
	public static MindHandle MIND_ATT_HANDLE_ID = Dust.lookup("1:6");

	public static MindHandle MIND_ASP_KNOWLEDGE = Dust.lookup("1:7");
	public static MindHandle MIND_ATT_KNOWLEDGE_HANDLE = Dust.lookup("1:8");
	public static MindHandle MIND_ATT_KNOWLEDGE_TAGS = Dust.lookup("1:9");
	public static MindHandle MIND_ATT_KNOWLEDGE_LISTENERS = Dust.lookup("1:10");
	public static MindHandle MIND_ATT_KNOWLEDGE_PRIMARYASPECT = Dust.lookup("1:11");

	public static MindHandle MIND_ASP_ASPECT = Dust.lookup("1:12");
	public static MindHandle MIND_ASP_ATTRIBUTE = Dust.lookup("1:13");
	public static MindHandle MIND_ASP_TAG = Dust.lookup("1:14");
	public static MindHandle MIND_ASP_LOGIC = Dust.lookup("1:15");
	public static MindHandle MIND_ASP_ASSEMBLY = Dust.lookup("1:16");
	public static MindHandle MIND_ATT_ASSEMBLY_UNITS = Dust.lookup("1:17");
	public static MindHandle MIND_ATT_ASSEMBLY_STARTAGENTS = Dust.lookup("1:18");

	public static MindHandle MIND_ASP_DIALOG = Dust.lookup("1:19");
	public static MindHandle MIND_ATT_DIALOG_ASSEMBLY = Dust.lookup("1:20");
	public static MindHandle MIND_ATT_DIALOG_KNOWLEDGE = Dust.lookup("1:21");
	public static MindHandle MIND_ATT_DIALOG_LAUNCHPARAMS = Dust.lookup("1:22");
	public static MindHandle MIND_ATT_DIALOG_ACTIVEAGENT = Dust.lookup("1:23");

	public static MindHandle MIND_ASP_AGENT = Dust.lookup("1:24");
	public static MindHandle MIND_ATT_AGENT_LOGIC = Dust.lookup("1:25");
	public static MindHandle MIND_ATT_AGENT_TARGET = Dust.lookup("1:26");

	public static MindHandle MIND_TAG_CONTEXT = Dust.lookup("1:27");
	public static MindHandle MIND_TAG_CONTEXT_DIALOG = Dust.lookup("1:28");
	public static MindHandle MIND_TAG_CONTEXT_SELF = Dust.lookup("1:29");
	public static MindHandle MIND_TAG_CONTEXT_TARGET = Dust.lookup("1:30");
	public static MindHandle MIND_TAG_CONTEXT_DIRECT = Dust.lookup("1:31");

	public static MindHandle MIND_TAG_VALTYPE = Dust.lookup("1:32");
	public static MindHandle MIND_TAG_VALTYPE_INT = Dust.lookup("1:33");
	public static MindHandle MIND_TAG_VALTYPE_REAL = Dust.lookup("1:34");
	public static MindHandle MIND_TAG_VALTYPE_HANDLE = Dust.lookup("1:35");
	public static MindHandle MIND_TAG_VALTYPE_BIN = Dust.lookup("1:36");

	public static MindHandle MIND_TAG_COLLTYPE = Dust.lookup("1:37");
	public static MindHandle MIND_TAG_COLLTYPE_ONE = Dust.lookup("1:38");
	public static MindHandle MIND_TAG_COLLTYPE_SET = Dust.lookup("1:39");
	public static MindHandle MIND_TAG_COLLTYPE_ARR = Dust.lookup("1:40");
	public static MindHandle MIND_TAG_COLLTYPE_MAP = Dust.lookup("1:41");

	public static MindHandle MIND_TAG_ACCESS = Dust.lookup("1:42");
	public static MindHandle MIND_TAG_ACCESS_CHECK = Dust.lookup("1:43");
	public static MindHandle MIND_TAG_ACCESS_PEEK = Dust.lookup("1:44");
	public static MindHandle MIND_TAG_ACCESS_GET = Dust.lookup("1:45");
	public static MindHandle MIND_TAG_ACCESS_SET = Dust.lookup("1:46");
	public static MindHandle MIND_TAG_ACCESS_INSERT = Dust.lookup("1:47");
	public static MindHandle MIND_TAG_ACCESS_DELETE = Dust.lookup("1:48");
	public static MindHandle MIND_TAG_ACCESS_RESET = Dust.lookup("1:49");
	public static MindHandle MIND_TAG_ACCESS_COMMIT = Dust.lookup("1:50");
	public static MindHandle MIND_TAG_ACCESS_BROADCAST = Dust.lookup("1:51");
	public static MindHandle MIND_TAG_ACCESS_LOOKUP = Dust.lookup("1:52");

	public static MindHandle MIND_TAG_ACTION = Dust.lookup("1:53");
	public static MindHandle MIND_TAG_ACTION_INIT = Dust.lookup("1:54");
	public static MindHandle MIND_TAG_ACTION_BEGIN = Dust.lookup("1:55");
	public static MindHandle MIND_TAG_ACTION_PROCESS = Dust.lookup("1:56");
	public static MindHandle MIND_TAG_ACTION_END = Dust.lookup("1:57");
	public static MindHandle MIND_TAG_ACTION_RELEASE = Dust.lookup("1:58");

	public static MindHandle MIND_TAG_STATUS = Dust.lookup("1:59");
	public static MindHandle MIND_TAG_STATUS_IDLE = Dust.lookup("1:60");
	public static MindHandle MIND_TAG_STATUS_PROCESSING = Dust.lookup("1:61");
	public static MindHandle MIND_TAG_STATUS_WAITING = Dust.lookup("1:62");
	public static MindHandle MIND_TAG_STATUS_ERROR = Dust.lookup("1:63");

	public static MindHandle MIND_TAG_RESULT = Dust.lookup("1:64");
	public static MindHandle MIND_TAG_RESULT_REJECT = Dust.lookup("1:65");
	public static MindHandle MIND_TAG_RESULT_PASS = Dust.lookup("1:66");
	public static MindHandle MIND_TAG_RESULT_READ = Dust.lookup("1:67");
	public static MindHandle MIND_TAG_RESULT_READACCEPT = Dust.lookup("1:68");
	public static MindHandle MIND_TAG_RESULT_ACCEPT = Dust.lookup("1:69");

	public static MindHandle MIND_ASP_AUTHOR = Dust.lookup("1:70");

	public static MindHandle MIND_AUTHOR_DUST = Dust.lookup("1:71");

	public static MindHandle DUST_UNIT = Dust.lookup("2");

	public static MindHandle DUST_ASP_MACHINE = Dust.lookup("2:1");
	public static MindHandle DUST_ATT_MACHINE_UNITS = Dust.lookup("2:2");
	public static MindHandle DUST_ATT_MACHINE_ASSEMBLIES = Dust.lookup("2:3");
	public static MindHandle DUST_ATT_MACHINE_MAINASSEMBLY = Dust.lookup("2:4");
	public static MindHandle DUST_ATT_MACHINE_MODULES = Dust.lookup("2:5");
	public static MindHandle DUST_ATT_MACHINE_DIALOGS = Dust.lookup("2:6");
	public static MindHandle DUST_ATT_MACHINE_MAINDIALOG = Dust.lookup("2:7");
	public static MindHandle DUST_ATT_MACHINE_THREADS = Dust.lookup("2:8");
	public static MindHandle DUST_ATT_MACHINE_ALL_IMPLEMENTATIONS = Dust.lookup("2:9");
	public static MindHandle DUST_ATT_MACHINE_ACTIVE_SERVERS = Dust.lookup("2:10");

	public static MindHandle DUST_ASP_MODULE = Dust.lookup("2:11");
	public static MindHandle DUST_ATT_MODULE_UNITS = Dust.lookup("2:12");
	public static MindHandle DUST_ATT_MODULE_NATIVELOGICS = Dust.lookup("2:13");
	public static MindHandle DUST_ATT_MODULE_LIBRARIES = Dust.lookup("2:14");

	public static MindHandle DUST_ASP_THREAD = Dust.lookup("2:15");
	public static MindHandle DUST_ATT_THREAD_DIALOG = Dust.lookup("2:16");

	public static MindHandle DUST_ASP_NATIVELOGIC = Dust.lookup("2:17");
	public static MindHandle DUST_ATT_NATIVELOGIC_LOGIC = Dust.lookup("2:18");
	public static MindHandle DUST_ATT_NATIVELOGIC_IMPLEMENTATION = Dust.lookup("2:19");
	public static MindHandle DUST_ATT_NATIVELOGIC_INSTANCE = Dust.lookup("2:20");

	public static MindHandle DUST_TAG_NATIVELOGIC_SERVER = Dust.lookup("2:21");

	public static MindHandle MISC_UNIT = Dust.lookup("3");

	public static MindHandle MISC_ASP_CONN = Dust.lookup("3:1");
	public static MindHandle MISC_ATT_CONN_OWNER = Dust.lookup("3:2");
	public static MindHandle MISC_ATT_CONN_PARENT = Dust.lookup("3:3");
	public static MindHandle MISC_ATT_CONN_SOURCE = Dust.lookup("3:4");
	public static MindHandle MISC_ATT_CONN_TARGET = Dust.lookup("3:5");
	public static MindHandle MISC_ATT_CONN_REQUIRES = Dust.lookup("3:6");
	public static MindHandle MISC_ATT_CONN_MEMBERMAP = Dust.lookup("3:7");
	public static MindHandle MISC_ATT_CONN_MEMBERARR = Dust.lookup("3:8");
	public static MindHandle MISC_ATT_CONN_MEMBERSET = Dust.lookup("3:9");

	public static MindHandle MISC_ASP_ALIAS = Dust.lookup("3:10");
	public static MindHandle MISC_ASP_GEN = Dust.lookup("3:11");
	public static MindHandle MISC_ATT_GEN_COUNT = Dust.lookup("3:12");

	public static MindHandle MISC_TAG_EMPTY = Dust.lookup("3:13");

	public static MindHandle MISC_TAG_ACTIVE = Dust.lookup("3:14");

	public static MindHandle MISC_TAG_DIRECTION = Dust.lookup("3:15");
	public static MindHandle MISC_TAG_DIRECTION_IN = Dust.lookup("3:16");
	public static MindHandle MISC_TAG_DIRECTION_OUT = Dust.lookup("3:17");

	public static MindHandle MISC_ASP_VARIANT = Dust.lookup("3:18");
	public static MindHandle MISC_ATT_VARIANT_VALUE = Dust.lookup("3:19");

	public static MindHandle TEXT_UNIT = Dust.lookup("4");

	public static MindHandle TEXT_TAG_LANGUAGE = Dust.lookup("4:1");
	public static MindHandle TEXT_TAG_LANGUAGE_EN_US = Dust.lookup("4:2");

	public static MindHandle TEXT_ATT_LANGUAGE_DEFAULT = Dust.lookup("4:3");
	public static MindHandle TEXT_ATT_TOKEN = Dust.lookup("4:4");

	public static MindHandle TEXT_TAG_TYPE = Dust.lookup("4:5");
	public static MindHandle TEXT_TAG_TYPE_TOKEN = Dust.lookup("4:6");
	public static MindHandle TEXT_TAG_TYPE_NAME = Dust.lookup("4:7");

	public static MindHandle TEXT_ASP_PLAIN = Dust.lookup("4:8");
	public static MindHandle TEXT_ATT_PLAIN_TEXT = Dust.lookup("4:9");

	public static MindHandle EVENT_UNIT = Dust.lookup("5");

	public static MindHandle EVENT_ASP_EVENT = Dust.lookup("5:1");

	public static MindHandle EVENT_TAG_TYPE = Dust.lookup("5:2");
	public static MindHandle EVENT_TAG_TYPE_EXCEPTIONTHROWN = Dust.lookup("5:3");
	public static MindHandle EVENT_TAG_TYPE_EXCEPTIONSWALLOWED = Dust.lookup("5:4");
	public static MindHandle EVENT_TAG_TYPE_ERROR = Dust.lookup("5:5");
	public static MindHandle EVENT_TAG_TYPE_WARNING = Dust.lookup("5:6");
	public static MindHandle EVENT_TAG_TYPE_INFO = Dust.lookup("5:7");
	public static MindHandle EVENT_TAG_TYPE_TRACE = Dust.lookup("5:8");
	public static MindHandle EVENT_TAG_TYPE_BREAKPOINT = Dust.lookup("5:9");

	public static MindHandle RESOURCE_UNIT = Dust.lookup("6");

	public static MindHandle RESOURCE_ASP_URL = Dust.lookup("6:1");
	public static MindHandle RESOURCE_ATT_URL_SCHEME = Dust.lookup("6:2");
	public static MindHandle RESOURCE_ATT_URL_USERINFO = Dust.lookup("6:3");
	public static MindHandle RESOURCE_ATT_URL_HOST = Dust.lookup("6:4");
	public static MindHandle RESOURCE_ATT_URL_PATH = Dust.lookup("6:5");
	public static MindHandle RESOURCE_ATT_URL_QUERY = Dust.lookup("6:6");
	public static MindHandle RESOURCE_ATT_URL_FRAGMENT = Dust.lookup("6:7");

	public static MindHandle RESOURCE_ASP_STREAM = Dust.lookup("6:8");

	public static MindHandle RESOURCE_TAG_STREAMTYPE = Dust.lookup("6:9");
	public static MindHandle RESOURCE_TAG_STREAMTYPE_RAW = Dust.lookup("6:10");
	public static MindHandle RESOURCE_TAG_STREAMTYPE_TEXT = Dust.lookup("6:11");
	public static MindHandle RESOURCE_TAG_STREAMTYPE_ZIPENTRY = Dust.lookup("6:12");

	public static MindHandle RESOURCE_ASP_PROCESSOR = Dust.lookup("6:13");
	public static MindHandle RESOURCE_ATT_PROCESSOR_STREAM = Dust.lookup("6:14");
	public static MindHandle RESOURCE_ATT_PROCESSOR_DATA = Dust.lookup("6:15");

	public static MindHandle RESOURCE_SRV_FILESYSTEM = Dust.lookup("6:16");
	public static MindHandle RESOURCE_AGT_ZIPREADER = Dust.lookup("6:17");
	public static MindHandle RESOURCE_AGT_JSONDOM = Dust.lookup("6:18");
	public static MindHandle RESOURCE_AGT_CSVSAX = Dust.lookup("6:19");

	public static MindHandle RESOURCE_ATT_CSVSAX_SEP = Dust.lookup("6:20");

	public static MindHandle DEV_UNIT = Dust.lookup("7");

	public static MindHandle DEV_ATT_HINT = Dust.lookup("7:1");

	public static MindHandle DUSTJAVA_UNIT = Dust.lookup("8");

	public static MindHandle L10N_UNIT = Dust.lookup("9");

}