package com.quakearts.auth.tests.authentication.test.client;

import com.quakearts.rest.client.HttpClientBuilder;

public class TestClientBuilder extends HttpClientBuilder<TestClient> {

	private TestClientBuilder() {}
	
	public static TestClientBuilder createNewTestClient() {
		TestClientBuilder instance = new TestClientBuilder();
		instance.httpClient = new TestClient();
		return instance;
	}
	
	@Override
	public TestClient thenBuild() {
		return httpClient;
	}

}
