package com.quakearts.auth.server.totp.edge.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.edge.client.TOTPServerHttpClient;
import com.quakearts.auth.server.totp.edge.client.model.ActivationRequest;
import com.quakearts.auth.server.totp.edge.client.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.edge.client.model.ProvisioningResponse;
import com.quakearts.auth.server.totp.edge.client.model.SyncResponse;
import com.quakearts.auth.server.totp.edge.exception.ConnectorException;
import com.quakearts.auth.server.totp.edge.test.runner.MainRunner;

@RunWith(MainRunner.class)
public class TOTPServerHttpClientTest 
	extends TestServerTest {

	@Inject
	private TOTPServerHttpClient client;
	
	@Test
	public void testProvisionActivateAndAuthentication() throws Exception {
		String deviceId = "NBV7-ST2R-3U47-6HFE-CSAQ-K9XC-NCJC-QZ4B";
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
	}

	@Test
	public void testSynchronize() throws Exception{
		SyncResponse syncResponse = client.synchronize();
		assertThat(syncResponse, is(notNullValue()));
		assertThat(syncResponse.getTime(), is(1566253087636l));
	}
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testErrorResponseGreaterThan299() throws Exception {
		expectedException.expect(ConnectorException.class);
		expectedException.expect(messageIs("Error message test"));
		client.provision(null);
	}
	
}
