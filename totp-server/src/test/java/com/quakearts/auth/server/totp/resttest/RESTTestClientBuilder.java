package com.quakearts.auth.server.totp.resttest;

import com.quakearts.rest.client.HttpClientBuilder;

public class RESTTestClientBuilder extends HttpClientBuilder<RESTTestClient> {

	public RESTTestClientBuilder() {
		httpClient = new RESTTestClient();
	}
	
	@Override
	public RESTTestClient thenBuild() {
		return httpClient;
	}

}
