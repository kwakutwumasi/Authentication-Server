package com.quakearts.auth.server.totp.options;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public interface TOTPLoginConfiguration {
	Map<String, ?> getConfigurationOptions() throws NoSuchAlgorithmException, URISyntaxException;
	Map<String, ?> getServerConfigurationOptions() throws NoSuchAlgorithmException, URISyntaxException;
	Map<String, ?> getSigningConfigurationOptions() throws NoSuchAlgorithmException, URISyntaxException;
}
