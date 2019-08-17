package com.quakearts.auth.server.totp.options;

import com.quakearts.appbase.internal.properties.ConfigurationPropertyMap;

public interface TOTPConfigurationProvider {
	ConfigurationPropertyMap getConfigurationPropertyMap();
	String getConfigurationPropertyMapName();
}
