package hu.sze.uni.http;

import java.nio.charset.StandardCharsets;

public interface DustHttpConsts {
	String CHARSET_UTF8 = StandardCharsets.UTF_8.name();// "UTF-8";

	String CONTENT_JSON = "application/json";
	String CONTENT_ZIP = "application/zip";
	String CONTENT_TEXT = "text/plain";
	String CONTENT_HTML = "text/html";
	String CONTENT_CSV = "text/csv";

	int NO_PORT_SET = -1;

	enum ServletData {
		Attribute, Parameter, Header, Method, Payload,
		Response, Status, ContentType, Charset, Command
	};

}
