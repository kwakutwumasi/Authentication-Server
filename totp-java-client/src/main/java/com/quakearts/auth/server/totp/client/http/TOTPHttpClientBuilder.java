package com.quakearts.auth.server.totp.client.http;

import java.net.URL;

import com.quakearts.rest.client.HttpClientBuilder;

public class TOTPHttpClientBuilder 
	extends HttpClientBuilder<TOTPHttpClient> {
	
	public TOTPHttpClientBuilder() {
		httpClient = new TOTPHttpClient();
	}
	
	public HttpClientBuilder<TOTPHttpClient> setFileAs(String file) {
		httpClient.file = file;
		return this;
	}
	
	@Override
	public HttpClientBuilder<TOTPHttpClient> setURLAs(URL url) {
		setFileAs(url.getFile());
		return super.setURLAs(url);
	}
	
	@Override
	public TOTPHttpClient thenBuild() {
		return httpClient;
	}
}
