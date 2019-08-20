package com.quakearts.auth.server.totp.edge.client;

import java.net.URL;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.auth.server.totp.options.TOTPEdgeOptions;
import com.quakearts.rest.client.HttpClientBuilder;

@Singleton
public class TOTPServerHttpClientBuilder 
	extends HttpClientBuilder<TOTPServerHttpClient> {

	@Inject
	private TOTPEdgeOptions totpEdgeOptions;
	
	public TOTPServerHttpClientBuilder() {
		httpClient = new TOTPServerHttpClient();
	}
	
	public HttpClientBuilder<TOTPServerHttpClient> setFileAs(String file) {
		httpClient.file = file;
		return this;
	}
	
	@Override
	public HttpClientBuilder<TOTPServerHttpClient> setURLAs(URL url) {
		setFileAs(url.getFile());
		return super.setURLAs(url);
	}
	
	@Override
	public TOTPServerHttpClient thenBuild() {
		return httpClient;
	}

	@Produces @Default
	public TOTPServerHttpClient createProvisioningServerHttpClient() {
		return setURLAs(totpEdgeOptions.getTotpUrl())
				.thenBuild();
	}
}
