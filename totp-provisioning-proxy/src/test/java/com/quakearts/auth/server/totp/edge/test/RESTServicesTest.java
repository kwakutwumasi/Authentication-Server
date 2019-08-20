package com.quakearts.auth.server.totp.edge.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

import java.lang.reflect.Field;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.edge.client.TOTPServerHttpClient;
import com.quakearts.auth.server.totp.edge.client.TOTPServerHttpClientBuilder;
import com.quakearts.auth.server.totp.edge.client.model.ActivationRequest;
import com.quakearts.auth.server.totp.edge.client.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.edge.client.model.ProvisioningResponse;
import com.quakearts.auth.server.totp.edge.client.model.SyncResponse;
import com.quakearts.auth.server.totp.edge.exception.ConnectorException;
import com.quakearts.auth.server.totp.edge.rest.AuthenticationResource;
import com.quakearts.auth.server.totp.edge.rest.ProvisioningResource;
import com.quakearts.auth.server.totp.edge.rest.SynchronizeResource;
import com.quakearts.auth.server.totp.edge.test.runner.MainRunner;
import com.quakearts.tools.test.mockserver.MockServer;
import com.quakearts.tools.test.mockserver.MockServerFactory;
import com.quakearts.tools.test.mockserver.configuration.Configuration.MockingMode;
import com.quakearts.tools.test.mockserver.configuration.impl.ConfigurationBuilder;
import com.quakearts.tools.test.mockserver.model.impl.HttpMessageBuilder;
import com.quakearts.tools.test.mockserver.model.impl.MockActionBuilder;

@RunWith(MainRunner.class)
public class RESTServicesTest extends TestServerTest {

	private static TOTPServerHttpClient client;
	private static MockServer errorThrowingServer;
	
	@BeforeClass
	public static void createClient() {
		client = new TOTPServerHttpClientBuilder()
				.setURLAs("http://localhost:8082/totp-provisioning")
				.thenBuild();
		
		errorThrowingServer = MockServerFactory
			.getInstance().getMockServer()
			.configure(ConfigurationBuilder.newConfiguration()
					.setMockingModeAs(MockingMode.MOCK)
					.setPortAs(8083).thenBuild())
			.add(MockActionBuilder.createNewMockAction()
					.setRequestAs(HttpMessageBuilder.createNewHttpRequest()
							.setId("provision")
							.setResourceAs("/totp/provisioning/NBV7-ST2R-3U47-6HFE-CSAQ-K9XC-NCJC-QZ4B")
							.setMethodAs("POST")
							.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
									.setContentBytes("{\"message\":\"Throwing provision error\"}".getBytes())
									.setResponseCodeAs(403)
									.thenBuild())
							.thenBuild())
					.thenBuild())
			.add(MockActionBuilder.createNewMockAction()
					.setRequestAs(HttpMessageBuilder.createNewHttpRequest()
							.setId("activate")
							.setResourceAs("/totp/provisioning/NBV7-ST2R-3U47-6HFE-CSAQ-K9XC-NCJC-QZ4B")
							.setMethodAs("PUT")
							.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
									.setContentBytes("{\"message\":\"Throwing activate error\"}".getBytes())
									.setResponseCodeAs(403)
									.thenBuild())
							.thenBuild())
					.thenBuild())
			.add(MockActionBuilder.createNewMockAction()
					.setRequestAs(HttpMessageBuilder.createNewHttpRequest()
							.setId("authenticate")
							.setResourceAs("/totp/authenticate")
							.setMethodAs("POST")
							.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
									.setContentBytes("{\"message\":\"Throwing authenticate error\"}".getBytes())
									.setResponseCodeAs(403)
									.thenBuild())
							.thenBuild())
					.thenBuild())
			.add(MockActionBuilder.createNewMockAction()
					.setRequestAs(HttpMessageBuilder.createNewHttpRequest()
							.setId("sync")
							.setResourceAs("/totp/sync")
							.setMethodAs("GET")
							.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
									.setContentBytes("{\"message\":\"Throwing sync error\"}".getBytes())
									.setResponseCodeAs(403)
									.thenBuild())
							.thenBuild())
					.thenBuild());
		
		errorThrowingServer.start();
		
		CDI.current().select(AuthenticationResource.class).get();
		CDI.current().select(ProvisioningResource.class).get();
		CDI.current().select(SynchronizeResource.class).get();
	}
	
	@AfterClass
	public static void shutdownErrorThrowingServer() {
		errorThrowingServer.stop();
	}
	
	@Inject
	private TOTPServerHttpClient serverClient;
	private String deviceId = "NBV7-ST2R-3U47-6HFE-CSAQ-K9XC-NCJC-QZ4B";
	
	@Test
	public void testRESTServices() throws Exception {
		ProvisioningResponse response = client.provision(deviceId);
		assertThat(response, is(notNullValue()));
		assertThat(response.getInitialCounter(), is(1566260812421L));
		assertThat(response.getSeed(), is("36762138fc61e60492dd922da534f9f8c2a60dcf71b4b43a02815e658dfef609"));
		ActivationRequest activationRequest = new ActivationRequest();
		activationRequest.setAlias("test-edge-device-1");
		activationRequest.setToken("346304");
		client.activate(deviceId, activationRequest);
		AuthenticationRequest authenticationRequest = new AuthenticationRequest();
		authenticationRequest.setDeviceId(deviceId);
		authenticationRequest.setOtp("346304");
		client.authentication(authenticationRequest);
		SyncResponse syncResponse = client.synchronize();
		assertThat(syncResponse, is(notNullValue()));
		assertThat(syncResponse.getTime(), is(1566253087636l));
	}

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testProvisionWithIOException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Connection refused: connect"));
		runWithNewPort(8084, ()->{
			client.provision(deviceId);
		});
	}
	
	@Test
	public void testProvisionWithConnectorException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Throwing provision error"));
		runWithNewPort(8083, ()->{
			client.provision(deviceId);
		});
	}

	
	@Test
	public void testProvisionWithHttpClientException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Property host has not been set"));
		runWithNewHost(null, ()->{
			client.provision(deviceId);
		});
	}
	
	@Test
	public void testActivateWithIOException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Connection refused: connect"));
		runWithNewPort(8084, ()->{
			ActivationRequest activationRequest = new ActivationRequest();
			activationRequest.setAlias("test-edge-device-1");
			activationRequest.setToken("346304");
			client.activate(deviceId, activationRequest);
		});
	}
	
	@Test
	public void testActivateWithConnectorException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Throwing activate error"));
		runWithNewPort(8083, ()->{
			ActivationRequest activationRequest = new ActivationRequest();
			activationRequest.setAlias("test-edge-device-1");
			activationRequest.setToken("346304");
			client.activate(deviceId, activationRequest);
		});
	}
	
	@Test
	public void testActivateWithHttpClientException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Property host has not been set"));
		runWithNewHost(null, ()->{
			ActivationRequest activationRequest = new ActivationRequest();
			activationRequest.setAlias("test-edge-device-1");
			activationRequest.setToken("346304");
			client.activate(deviceId, activationRequest);
		});
	}

	@Test
	public void testAuthenticateWithIOException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Connection refused: connect"));
		runWithNewPort(8084, ()->{
			AuthenticationRequest authenticationRequest = new AuthenticationRequest();
			authenticationRequest.setDeviceId(deviceId);
			authenticationRequest.setOtp("346304");
			client.authentication(authenticationRequest);
		});
	}
	
	@Test
	public void testAuthenticateWithConnectorException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Throwing authenticate error"));
		runWithNewPort(8083, ()->{
			AuthenticationRequest authenticationRequest = new AuthenticationRequest();
			authenticationRequest.setDeviceId(deviceId);
			authenticationRequest.setOtp("346304");
			client.authentication(authenticationRequest);
		});
	}
	
	@Test
	public void testAuthenticateWithHttpClientException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Property host has not been set"));
		runWithNewHost(null, ()->{
			AuthenticationRequest authenticationRequest = new AuthenticationRequest();
			authenticationRequest.setDeviceId(deviceId);
			authenticationRequest.setOtp("346304");
			client.authentication(authenticationRequest);
		});
	}

	@Test
	public void testSynchronizeWithIOException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Connection refused: connect"));
		runWithNewPort(8084, ()->{
			client.synchronize();
		});
	}
	
	@Test
	public void testSynchronizeWithConnectorException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Throwing sync error"));
		runWithNewPort(8083, ()->{
			client.synchronize();
		});
	}
	
	@Test
	public void testSynchronizeWithHttpClientException() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Property host has not been set"));
		runWithNewHost(null, ()->{
			client.synchronize();
		});
	}

	private void runWithNewHost(String newHost, 
			TestFunction function) throws Exception {
		String oldHost = changeServerHost(newHost);
		try {
			function.apply();
		} finally {
			changeServerHost(oldHost);
		}
	}
	
	private String changeServerHost(String newHost) 
			throws Exception {
		Field hostField = TOTPServerHttpClient.class
				.getSuperclass().getDeclaredField("host");
		hostField.setAccessible(true);
		String oldValue = (String) hostField.get(serverClient);
		hostField.set(serverClient, newHost);
		return oldValue;
	}
	
	private void runWithNewPort(int newPort, 
			TestFunction function) throws Exception {
		int oldPort = changeServerPort(newPort);
		try {
			function.apply();
		} finally {
			changeServerPort(oldPort);
		}
	}
	
	private int changeServerPort(int newPort) 
			throws Exception {
		Field portField = TOTPServerHttpClient.class
				.getSuperclass().getDeclaredField("port");
		portField.setAccessible(true);
		int oldValue = portField.getInt(serverClient);
		portField.set(serverClient, newPort);
		return oldValue;
	}
	
	interface TestFunction {
		void apply() throws Exception;
	}

}
