package hu.sze.uni.http;

import java.nio.charset.StandardCharsets;

public interface DustHttpConsts {
	String CHARSET_UTF8 = StandardCharsets.UTF_8.name();// "UTF-8";

	String CONTENT_ZIP = "application/zip";

	String CONTENT_JSON = "application/json; charset=UTF-8";
	String CONTENT_TEXT = "text/plain; charset=UTF-8";
	String CONTENT_HTML = "text/html; charset=UTF-8";
	String CONTENT_CSV = "text/csv; charset=UTF-8";

	int NO_PORT_SET = -1;

	enum ServletData {
		Attribute, Parameter, Header, Method, Payload,
		Response, Status, ContentType, Charset, Command
	};

}
