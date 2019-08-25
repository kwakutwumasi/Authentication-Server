package com.quakearts.auth.server.totp.options.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.URIParameter;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import com.quakearts.auth.server.totp.options.TOTPLoginConfiguration;
import com.quakearts.auth.server.totp.rest.filter.TOTPAuthenticationFilter;

public class TOTPLoginConfigurationImpl implements TOTPLoginConfiguration {

	private Map<String, ?> options;

	@Override
	public Map<String, ?> getConfigurationOptions() throws NoSuchAlgorithmException, URISyntaxException {
		if(options==null){
			URL resource = Thread.currentThread().getContextClassLoader().
                    getResource("login.config");
            URI uri = resource.toURI();
            Configuration jaasConfig = Configuration.getInstance("JavaLoginConfig", 
            		new URIParameter(uri));
			AppConfigurationEntry entry = jaasConfig.getAppConfigurationEntry(TOTPAuthenticationFilter.LOGIN_MODULE)[0];
			options = entry.getOptions();
		}
		
		return options;
	}

}
