package com.quakearts.auth.server.proxy.test;

import org.hamcrest.Matcher;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.proxy.client.exception.ConnectorException;
import com.quakearts.auth.server.proxy.client.model.ErrorResponse;
import com.quakearts.auth.server.proxy.test.client.TestHttpClient;
import com.quakearts.auth.server.proxy.test.client.TestHttpClientBuilder;
import com.quakearts.auth.server.proxy.test.runner.MainRunner;

@RunWith(MainRunner.class)
public class AuthenticationEdgeApplicationExceptionTest {
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testRestInterfaceWithIOException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(responseMatches(new ErrorResponse()
				.withCodeAs("java.net.ConnectException")
				.addExplanation("Connection refused: connect"), 504));
		TestHttpClient client =  new TestHttpClientBuilder()
				.setURLAs("http://localhost:8080").thenBuild();
		client.authenticate("test-main", "Test", "testuser", "dGVzdDE=");
	}

	@Test
	public void testRestInterfaceWithNoCredentials() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(responseMatches(new ErrorResponse()
				.withCodeAs("java.lang.IllegalArgumentException")
				.addExplanation("clientId is required"), 500));
		TestHttpClient client =  new TestHttpClientBuilder()
				.setURLAs("http://localhost:8080").thenBuild();
		client.emptyAuthentication("test-main", "Test");
	}
	
	private Matcher<ConnectorException> responseMatches(ErrorResponse errorResponse, int httpCode){
		return new HttpResponseMatcher(errorResponse, httpCode);
	}
}
