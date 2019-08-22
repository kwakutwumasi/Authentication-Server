package com.quakearts.auth.server.proxy.client;

import java.net.URL;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import com.quakearts.rest.client.HttpClientBuilder;

public class AuthenticationServerHttpClientBuilder extends HttpClientBuilder<AuthenticationServerHttpClient> {

	public AuthenticationServerHttpClientBuilder() {
		httpClient = new AuthenticationServerHttpClient();
	}
	
	public HttpClientBuilder<AuthenticationServerHttpClient> setFileAs(String file) {
		httpClient.file = file;
		return this;
	}
	
	@Override
	public HttpClientBuilder<AuthenticationServerHttpClient> setURLAs(URL url) {
		setFileAs(url.getFile());
		return super.setURLAs(url);
	}
	
	@Override
	public AuthenticationServerHttpClient thenBuild() {
		return httpClient;
	}

	@Produces @Default
	public AuthenticationServerHttpClient createProvisioningServerHttpClient() {
		return setURLAs(System.getProperty("totp.url","http://localhost:8080/totp"))
				.thenBuild();
	}
}
