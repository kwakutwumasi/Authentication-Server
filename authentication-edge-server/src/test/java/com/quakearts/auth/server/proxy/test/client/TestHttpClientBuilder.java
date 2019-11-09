package com.quakearts.auth.server.proxy.test.client;

import com.quakearts.rest.client.HttpClientBuilder;

public class TestHttpClientBuilder extends HttpClientBuilder<TestHttpClient> {

	public TestHttpClientBuilder() {
		httpClient = new TestHttpClient();
	}
	
	@Override
	public TestHttpClient thenBuild() {
		return httpClient;
	}
}
