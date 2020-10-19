package com.quakearts.auth.server.totp.options.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.URIParameter;
import java.util.Map;

import javax.inject.Inject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import com.quakearts.auth.server.totp.options.TOTPLoginConfiguration;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.auth.server.totp.rest.filter.TOTPAuthenticationFilter;

public class TOTPLoginConfigurationImpl implements TOTPLoginConfiguration {

	@Inject
	private TOTPOptions totpOptions;
	
	private Map<String, ?> options;
	private Map<String, ?> serverOptions;
	private Map<String, ?> signingOptions;
	
	@Override
	public Map<String, ?> getConfigurationOptions() throws NoSuchAlgorithmException, URISyntaxException {
		if(options==null){
			options = loadOptions("login.config");
		}
		
		return options;
	}

	@Override
	public Map<String, ?> getServerConfigurationOptions() throws NoSuchAlgorithmException, URISyntaxException {
		if(serverOptions==null){
			serverOptions = loadOptions(totpOptions.getServerJwtConfigName());
		}
		
		return serverOptions;
	}
	
	@Override
	public Map<String, ?> getSigningConfigurationOptions() throws NoSuchAlgorithmException, URISyntaxException {
		if(signingOptions==null){
			signingOptions = loadOptions(totpOptions.getRequestSigningJwtConfigName());
		}
		
		return signingOptions;
	}
	
	protected Map<String, ?> loadOptions(String resourceName) throws URISyntaxException, NoSuchAlgorithmException {
		URL resource = Thread.currentThread().getContextClassLoader().
		        getResource(resourceName);
		URI uri = resource.toURI();
		Configuration jaasConfig = Configuration.getInstance("JavaLoginConfig", 
				new URIParameter(uri));
		AppConfigurationEntry entry = jaasConfig.getAppConfigurationEntry(TOTPAuthenticationFilter.LOGIN_MODULE)[0];
		return entry.getOptions();
	}
}
