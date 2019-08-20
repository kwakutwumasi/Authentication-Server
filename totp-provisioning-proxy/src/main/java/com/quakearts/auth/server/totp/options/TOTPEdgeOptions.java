package com.quakearts.auth.server.totp.options;

import java.util.Map;

public interface TOTPEdgeOptions {
	String getTotpUrl();
	String getTotpServerIp();
	int getTotpServerPort();
	String getKeystoreType();
	String getKeystore();
	String getKeystorePassword();
	String getSslInstance();
	String getKeyPassword();
	Map<String, ?> getJwtOptions();
	String getJwtalgorithm();
	long getPayloadQueueTimeout();
	int getPayloadQueueSize();
}