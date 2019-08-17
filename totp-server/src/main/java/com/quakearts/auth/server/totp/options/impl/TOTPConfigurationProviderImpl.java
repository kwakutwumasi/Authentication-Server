package com.quakearts.auth.server.totp.options.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.quakearts.appbase.Main;
import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.appbase.internal.properties.ConfigurationPropertyMap;
import com.quakearts.auth.server.totp.options.TOTPConfigurationProvider;

public class TOTPConfigurationProviderImpl implements TOTPConfigurationProvider {

	private static final String TOTPOPTIONS_JSON = "totpoptions.json";

	@Override
	public ConfigurationPropertyMap getConfigurationPropertyMap() {
		try {
			InputStream inputStream = Thread.currentThread()
					.getContextClassLoader().getResourceAsStream(TOTPOPTIONS_JSON);
			return Main.getAppBasePropertiesLoader()
					.loadParametersFromReader(TOTPOPTIONS_JSON, new InputStreamReader(inputStream));
		} catch (IOException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public String getConfigurationPropertyMapName() {
		return TOTPOPTIONS_JSON;
	}

}
