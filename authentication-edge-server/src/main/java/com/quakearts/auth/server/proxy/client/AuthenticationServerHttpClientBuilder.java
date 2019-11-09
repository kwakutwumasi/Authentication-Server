package com.quakearts.auth.server.proxy.client;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import com.quakearts.rest.client.HttpClientBuilder;

public class AuthenticationServerHttpClientBuilder extends HttpClientBuilder<AuthenticationServerHttpClient> {

	public AuthenticationServerHttpClientBuilder() {
		httpClient = new AuthenticationServerHttpClient();
	}
	
	@Override
	public AuthenticationServerHttpClient thenBuild() {
		return httpClient;
	}

	@Produces @Default
	public AuthenticationServerHttpClient createProvisioningServerHttpClient() {
		return setURLAs(System.getProperty("authentication.server.url","http://localhost:8180"))
				.thenBuild();
	}
}
