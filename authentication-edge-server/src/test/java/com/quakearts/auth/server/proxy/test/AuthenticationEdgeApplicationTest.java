package com.quakearts.auth.server.proxy.test;

import static org.junit.Assert.*;

import org.hamcrest.Matcher;

import static org.hamcrest.core.Is.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.proxy.client.exception.ConnectorException;
import com.quakearts.auth.server.proxy.client.model.ErrorResponse;
import com.quakearts.auth.server.proxy.client.model.TokenResponse;
import com.quakearts.auth.server.proxy.test.client.TestHttpClient;
import com.quakearts.auth.server.proxy.test.client.TestHttpClientBuilder;
import com.quakearts.auth.server.proxy.test.runner.MainRunner;
import com.quakearts.tools.test.mockserver.MockServer;
import com.quakearts.tools.test.mockserver.MockServerFactory;
import com.quakearts.tools.test.mockserver.configuration.Configuration.MockingMode;
import com.quakearts.tools.test.mockserver.configuration.impl.ConfigurationBuilder;
import com.quakearts.tools.test.mockserver.model.impl.HttpHeaderImpl;
import com.quakearts.tools.test.mockserver.model.impl.HttpMessageBuilder;
import com.quakearts.tools.test.mockserver.model.impl.MockActionBuilder;
import com.quakearts.tools.test.mockserver.store.HttpMessageStore;
import com.quakearts.tools.test.mockserver.store.impl.MockServletHttpMessageStore;

@RunWith(MainRunner.class)
public class AuthenticationEdgeApplicationTest {

	private static MockServer mockServer;
	private static TestHttpClient client;
	
	@BeforeClass
	public static void startServer() throws Exception {		
		HttpMessageStore httpMessageStore = MockServletHttpMessageStore.getInstance();
		
		mockServer = MockServerFactory
				.getInstance()
				.getMockServer()
				.configure(ConfigurationBuilder
						.newConfiguration()
						.setMockingModeAs(MockingMode.MOCK)
						.setPortAs(8180)
						.thenBuild())
				.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(httpMessageStore.findRequestIdentifiedBy("testAuthenticateOk"))
						.thenBuild())
				.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(httpMessageStore.findRequestIdentifiedBy("testAuthenticateWrongPassword"))
						.thenBuild())
				.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(HttpMessageBuilder
								.createNewHttpRequest()
									.setResourceAs("/authenticate/test-main/Test/?clientId=testuser&credential=wrong-response")
									.setMethodAs("GET")
									.setId("testAuthenticateWithWrongResponse")
									.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
											.addHeaders(new HttpHeaderImpl("Content-Type","application/json"))
											.setResponseCodeAs(200)
											.setContentBytes("{".getBytes())
											.thenBuild())
								.thenBuild())
						.thenBuild())
				.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(HttpMessageBuilder
								.createNewHttpRequest()
									.setResourceAs("/authenticate/test-main/Test/?clientId=testuser&credential=wrong-error-response")
									.setMethodAs("GET")
									.setId("testAuthenticateWithWrongResponse")
									.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
											.addHeaders(new HttpHeaderImpl("Content-Type","application/json"))
											.setResponseCodeAs(404)
											.setContentBytes("{".getBytes())
											.thenBuild())
								.thenBuild())
						.thenBuild());
		mockServer.start();
		
		client = new TestHttpClientBuilder()
				.setURLAs("http://localhost:8080").thenBuild();
	}
	
	@AfterClass
	public static void stopServer() {
		mockServer.stop();
	}
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testRestInterface() throws Exception {
		TokenResponse response = client.authenticate("test-main", "Test", "testuser", "dGVzdDE=");
		
		assertThat(response.getExpiresIn(), is(900L));
		assertThat(response.getIdToken(), is("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
				+ ".eyJpYXQiOjE1NzMzMjM0NTksImF1ZCI6Imh0dHBzOi8vcXVha2VhcnRzLmNvbSI"
				+ "sImlzcyI6Imh0dHBzOi8vcXVha2VhcnRzLmNvbSIsImV4cCI6MTU3MzMyNDM1OSw"
				+ "ic3ViIjoidGVzdHVzZXIiLCJ1c2VybmFtZSI6InRlc3R1c2VyIiwidGVzdCI6InZhbHVlIn0"
				+ ".2qUUeZPJ2tyjKhhyeH2RKBxG7YlFm32tFH0cAA7t91c"));
		assertThat(response.getTokenType(), is("bearer"));
	}
	
	@Test
	public void testRestInterfaceWithWrongPassword() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(responseMatches(new ErrorResponse()
				.withCodeAs("invalid-credentials")
				.addExplanation("The provided credentials could not be authenticated"),400));
		client.authenticate("test-main", "Test", "testuser", "wrong-password");
	}
	
	@Test
	public void testRestInterfaceWrongResponse() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(responseMatches(new ErrorResponse()
				.withCodeAs("com.quakearts.rest.client.exception.HttpClientException")
				.addExplanation("Unable to de-serialize response")
				.addExplanation("Unexpected end-of-input: expected close marker for Object (start marker at [Source: (byte[])\"{\"; line: 1, column: 1])\n" + 
						" at [Source: (byte[])\"{\"; line: 1, column: 2]"), 500));
		client.authenticate("test-main", "Test", "testuser", "wrong-response");
	}
	
	@Test
	public void testRestInterfaceWrongErrorResponse() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(responseMatches(new ErrorResponse()
				.withCodeAs("deserialization-error")
				.addExplanation("{"),404));
		client.authenticate("test-main", "Test", "testuser", "wrong-error-response");
	}
	
	private Matcher<ConnectorException> responseMatches(ErrorResponse errorResponse, int httpCode){
		return new HttpResponseMatcher(errorResponse, httpCode);
	}
}
