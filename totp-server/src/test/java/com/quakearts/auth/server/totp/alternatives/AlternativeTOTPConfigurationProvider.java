package com.quakearts.auth.server.totp.alternatives;

import com.quakearts.appbase.internal.properties.ConfigurationPropertyMap;
import com.quakearts.auth.server.totp.options.TOTPConfigurationProvider;

public class AlternativeTOTPConfigurationProvider implements TOTPConfigurationProvider {

	public AlternativeTOTPConfigurationProvider(ConfigurationPropertyMap configurationPropertyMap,
			String configurationPropertyMapName) {
		this.configurationPropertyMap = configurationPropertyMap;
		this.configurationPropertyMapName = configurationPropertyMapName;
	}

	private ConfigurationPropertyMap configurationPropertyMap;
	private String configurationPropertyMapName;

	@Override
	public ConfigurationPropertyMap getConfigurationPropertyMap() {
		return configurationPropertyMap;
	}

	@Override
	public String getConfigurationPropertyMapName() {
		return configurationPropertyMapName;
	}

}
