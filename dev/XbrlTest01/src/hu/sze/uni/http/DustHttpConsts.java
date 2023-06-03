package hu.sze.uni.http;

import java.nio.charset.StandardCharsets;

public interface DustHttpConsts {
	String CHARSET_UTF8 = StandardCharsets.UTF_8.name();// "UTF-8";

	String CONTENT_JSON = "application/json";
	int NO_PORT_SET = -1;

	enum DustNetTypes {
		NetAddress, NetClient, NetServer, NetSslInfo, NetProcessor, NetGetRef, NetProcessContext
	};

	enum DustNetAtts {
		NetAddressUrl, NetServerPublicPort, NetServerSslPort, NetSslInfoStorePath, NetSslInfoStorePass, NetSslInfoManagerPass, NetClientModuleToUpdate, NetProcessorRespCharset,
		NetProcessorRespContentType, NetProcessContextRequest, NetProcessContextResponse, NetProcessMethod
	}

	enum DustNetLinks {
		NetClientProxyEntities, NetProcessorParamTypes,
	};

	enum DustNetServices {
		NetClient, NetServer, NetGetRef
	};

}
