package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;
import static org.awaitility.Awaitility.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;

import org.awaitility.Duration;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.junit.BeforeClass;
import org.junit.Rule;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeAuthenticationService;
import com.quakearts.auth.server.totp.alternatives.AlternativeConnectionManager;
import com.quakearts.auth.server.totp.alternatives.AlternativeDeviceAuthorizationService;
import com.quakearts.auth.server.totp.alternatives.AlternativeDeviceRequestSigningService;
import com.quakearts.auth.server.totp.alternatives.AlternativeTOTPOptions;
import com.quakearts.auth.server.totp.alternatives.AlternativeDeviceManagementService;
import com.quakearts.auth.server.totp.exception.MessageGenerationException;
import com.quakearts.auth.server.totp.generator.TOTPGenerator;
import com.quakearts.auth.server.totp.generator.impl.JWTGeneratorImpl;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.rest.authorization.AuthorizeManagedRequestInterceptor;
import com.quakearts.auth.server.totp.rest.event.ProvisioningEvent;
import com.quakearts.auth.server.totp.rest.model.ActivationRequest;
import com.quakearts.auth.server.totp.rest.model.AdministratorResponse;
import com.quakearts.auth.server.totp.rest.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.rest.model.CountResponse;
import com.quakearts.auth.server.totp.rest.model.DeviceRequest;
import com.quakearts.auth.server.totp.rest.model.DeviceResponse;
import com.quakearts.auth.server.totp.rest.model.DirectAuthenticationRequest;
import com.quakearts.auth.server.totp.rest.model.ManagementRequest;
import com.quakearts.auth.server.totp.rest.model.ManagementResponse;
import com.quakearts.auth.server.totp.rest.model.ProvisioningResponse;
import com.quakearts.auth.server.totp.rest.model.TokenResponse;
import com.quakearts.auth.server.totp.resttest.RESTTestClient;
import com.quakearts.auth.server.totp.resttest.RESTTestClient.SyncResponse;
import com.quakearts.auth.server.totp.resttest.RESTTestClientBuilder;
import com.quakearts.auth.server.totp.runner.TOTPDatabaseServiceRunner;
import com.quakearts.rest.client.exception.HttpClientException;
import com.quakearts.security.cryptography.CryptoResource;
import com.quakearts.security.cryptography.exception.IllegalCryptoActionException;
import com.quakearts.security.cryptography.jpa.EncryptedValue;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;
import com.quakearts.webapp.orm.exception.DataStoreException;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.webapp.security.rest.exception.RestSecurityException;

@RunWith(TOTPDatabaseServiceRunner.class)
public class RESTServiceTest {
	
	@BeforeClass
	public static void createDatabase(){
		client = new RESTTestClientBuilder()
				.setURLAs("http://localhost:8080")
				.thenBuild();
	}
	
	@Inject
	private TOTPGenerator totpGenerator;
	private static RESTTestClient client;
	private static Device device1;
	private static Device device2;
	private static Device device3;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
		
	@Inject
	private JWTGeneratorImpl jwtGenerator;
	
	private static ProvisioningEvent provisioningEvent;
	
	public void firedProvisionEvent(@ObservesAsync ProvisioningEvent provisioningEvent) {
		RESTServiceTest.provisioningEvent = provisioningEvent;
	}
	
	@Test
	public void run200OkRequests() throws Exception {
		Device device1 = provisionTestProvisionDevice1();
		
		AlternativeAuthenticationService.returnLocked(null);
		AlternativeAuthenticationService.returnAuthenticate(null);
		
		String[] totp1 = totpGenerator.generateFor(device1, System.currentTimeMillis());
		
		AuthenticationRequest authenticateRequest = new AuthenticationRequest();
		
		authenticateRequest.setDeviceId("testprovisiondevice1");
		authenticateRequest.setOtp(totp1[0]);
		
		client.authenticate(authenticateRequest);
		AlternativeDeviceAuthorizationService.throwException(null);
		AlternativeConnectionManager.run(bite->{
			try {
				JWTClaims claims = jwtGenerator.verifyJWT(bite);
				assertThat(claims.getPrivateClaim("key"), is("value"));
				Map<String, String> responseMap = new HashMap<>();
				String[] direct = totpGenerator.generateFor(device1, System.currentTimeMillis());
				responseMap.put("otp", direct[0]);
				return jwtGenerator.generateJWT(responseMap).getBytes();
			} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
				throw new AssertionError(e);
			}
		});
		
		DirectAuthenticationRequest request = new DirectAuthenticationRequest();
		request.setDeviceId(device1.getId());
		request.setAuthenticationData(new HashMap<>());
		request.getAuthenticationData().put("key", "value");
		
		client.authenticateDirect(request);
		
		AlternativeConnectionManager.run(bite->{
			Map<String, String> responseMap = new HashMap<>();
			String signature = totpGenerator.signRequest(device1, "amount30requestvalue");
			responseMap.put("signature", signature);
			try {
				return jwtGenerator.generateJWT(responseMap).getBytes();
			} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
				throw new AssertionError(e);
			}
		});
		
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("request", "value");
		requestMap.put("amount", "30");
		TokenResponse tokenResponse = client.signRequest(device1.getId(), requestMap);
		assertThat(tokenResponse, is(notNullValue()));
		
		client.verifySignedRequest(tokenResponse.getToken(), device1.getId());
		
		Device device2 = provisionAdministrator1();
		Device device3 = provisionAdministrator2();
		
		String[] totp2 = totpGenerator.generateFor(device2, System.currentTimeMillis());

		AuthenticationRequest authorizationRequest = new AuthenticationRequest();
		authorizationRequest.setDeviceId("testadministrator1");
		authorizationRequest.setOtp(totp2[0]);
				
		tokenResponse = client.login(authorizationRequest);
		assertThat(tokenResponse.getToken(), is(notNullValue()));
		
		String[] totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest = new AuthenticationRequest();
		authorizationRequest.setDeviceId("testadministrator2");
		authorizationRequest.setOtp(totp3[0]);
		
		DeviceRequest deviceRequest1 = new DeviceRequest();
		deviceRequest1.setAlias("testrestassign1");
		deviceRequest1.setDeviceId("testprovisiondevice1");

		DeviceRequest deviceRequest2 = new DeviceRequest();
		deviceRequest2.setAlias("testrestassignnonexistentdevice1");
		deviceRequest2.setDeviceId("testnonexistentdevice1");

		DeviceRequest deviceRequest3 = new DeviceRequest();
		deviceRequest3.setAlias("testalias1");
		deviceRequest3.setDeviceId("testdevice1");
		
		DeviceRequest deviceRequest4 = new DeviceRequest();
		deviceRequest4.setDeviceId("testdevice1");
		
		ManagementRequest assignRequest = new ManagementRequest();
		assignRequest.setAuthorizationRequest(authorizationRequest);
		assignRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, deviceRequest3, deviceRequest4});
		
		ManagementResponse response = client.assignAliases(assignRequest);
		assertThat(response.getResponses(), is(notNullValue()));
		assertThat(response.getResponses().length, is(4));
		assertThat(response.getResponses()[0].getMessage(), is("Success"));
		assertThat(response.getResponses()[0].isError(), is(false));		
		assertThat(response.getResponses()[1].getMessage(), is("Error: Device was not found"));
		assertThat(response.getResponses()[1].isError(), is(true));
		assertThat(response.getResponses()[2].getMessage(), is("Error: The alias testalias1 has already been assigned"));
		assertThat(response.getResponses()[2].isError(), is(true));
		assertThat(response.getResponses()[3].getMessage(), is("Error: The alias null is not valid"));
		assertThat(response.getResponses()[3].isError(), is(true));
		
		deviceRequest1.setDeviceId(null);
		deviceRequest2.setDeviceId(null);
		
		totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest.setOtp(totp3[0]);

		ManagementRequest unassignRequest = new ManagementRequest();
		unassignRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2});		
		unassignRequest.setAuthorizationRequest(authorizationRequest);
		
		response = client.unassignAliases(unassignRequest);
		assertThat(response.getResponses(), is(notNullValue()));
		assertThat(response.getResponses().length, is(2));
		assertThat(response.getResponses()[0].getMessage(), is("Success"));
		assertThat(response.getResponses()[0].isError(), is(false));		
		assertThat(response.getResponses()[1].getMessage(), is("Error: The alias testrestassignnonexistentdevice1 was not assigned"));
		assertThat(response.getResponses()[1].isError(), is(true));		

		totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest.setOtp(totp3[0]);

		deviceRequest1.setAlias(null);
		deviceRequest1.setDeviceId("testunlockeddevice2");
		deviceRequest2.setAlias(null);
		deviceRequest2.setDeviceId("testinactivedevice1");
		deviceRequest3.setAlias(null);
		deviceRequest3.setDeviceId("testnonexistentdevice1");
		
		ManagementRequest lockRequest = new ManagementRequest();
		lockRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, deviceRequest3});		
		lockRequest.setAuthorizationRequest(authorizationRequest);
		
		response = client.lock(lockRequest);
		assertThat(response.getResponses(), is(notNullValue()));
		assertThat(response.getResponses().length, is(3));
		assertThat(response.getResponses()[0].getMessage(), is("Success"));
		assertThat(response.getResponses()[0].isError(), is(false));		
		assertThat(response.getResponses()[1].getMessage(), is("Error: The device testinactivedevice1 was not locked"));
		assertThat(response.getResponses()[1].isError(), is(true));		
		assertThat(response.getResponses()[2].getMessage(), is("Error: Device was not found"));
		assertThat(response.getResponses()[2].isError(), is(true));		

		totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest.setOtp(totp3[0]);
		ManagementRequest unlockRequest = new ManagementRequest();
		unlockRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, deviceRequest3});		
		unlockRequest.setAuthorizationRequest(authorizationRequest);
		
		response = client.unlock(unlockRequest);
		assertThat(response.getResponses(), is(notNullValue()));
		assertThat(response.getResponses().length, is(3));
		assertThat(response.getResponses()[0].getMessage(), is("Success"));
		assertThat(response.getResponses()[0].isError(), is(false));		
		assertThat(response.getResponses()[1].getMessage(), is("Error: The device testinactivedevice1 cannot be unlocked. It is INACTIVE"));
		assertThat(response.getResponses()[1].isError(), is(true));		
		assertThat(response.getResponses()[2].getMessage(), is("Error: Device was not found"));
		assertThat(response.getResponses()[2].isError(), is(true));		

		totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest.setOtp(totp3[0]);
		
		deviceRequest1.setDeviceId("testprovisiondevice1");
		deviceRequest1.setAlias("Administrator 3");
		deviceRequest2.setDeviceId("testadministrator2");
		deviceRequest2.setAlias("Administrator 4");
		deviceRequest3.setAlias("Administrator 5");
		
		deviceRequest4.setDeviceId("testinactivedevice1");

		DeviceRequest deviceRequest5 = new DeviceRequest();
		deviceRequest5.setAlias("Administrator 6");
		deviceRequest5.setDeviceId("testinactivedevice1");
		
		ManagementRequest addAsAdminRequest = new ManagementRequest();
		addAsAdminRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, 
				deviceRequest3, deviceRequest4, deviceRequest5});		
		addAsAdminRequest.setAuthorizationRequest(authorizationRequest);
		
		response = client.addAsAdmin(addAsAdminRequest);
		assertThat(response.getResponses(), is(notNullValue()));
		assertThat(response.getResponses().length, is(5));
		assertThat(response.getResponses()[0].getMessage(), is("Success"));
		assertThat(response.getResponses()[0].isError(), is(false));		
		assertThat(response.getResponses()[1].getMessage(), is("Error: The device testadministrator2 is already an administrator device"));
		assertThat(response.getResponses()[1].isError(), is(true));		
		assertThat(response.getResponses()[2].getMessage(), is("Error: Device was not found"));
		assertThat(response.getResponses()[2].isError(), is(true));		
		assertThat(response.getResponses()[3].getMessage(), is("Error: Common name was missing"));
		assertThat(response.getResponses()[3].isError(), is(true));		
		assertThat(response.getResponses()[4].getMessage(), is("Error: The device is not in a valid state. Status is INACTIVE"));
		assertThat(response.getResponses()[4].isError(), is(true));		

		totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest.setOtp(totp3[0]);
		
		deviceRequest1.setAlias(null);
		deviceRequest2.setAlias(null);
		deviceRequest3.setAlias(null);
		deviceRequest4.setAlias(null);
		
		ManagementRequest removeAsAdminRequest = new ManagementRequest();
		removeAsAdminRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, deviceRequest3, deviceRequest4});		
		removeAsAdminRequest.setAuthorizationRequest(authorizationRequest);
		
		response = client.removeAsAdmin(removeAsAdminRequest);
		assertThat(response.getResponses(), is(notNullValue()));
		assertThat(response.getResponses().length, is(4));
		assertThat(response.getResponses()[0].getMessage(), is("Success"));
		assertThat(response.getResponses()[0].isError(), is(false));		
		assertThat(response.getResponses()[1].getMessage(), is("Error: The device is an installed administrator and cannot be removed"));
		assertThat(response.getResponses()[1].isError(), is(true));		
		assertThat(response.getResponses()[2].getMessage(), is("Error: Device was not found"));
		assertThat(response.getResponses()[2].isError(), is(true));		
		assertThat(response.getResponses()[3].getMessage(), is("Error: The device testinactivedevice1 is not an administrator device"));
		assertThat(response.getResponses()[3].isError(), is(true));		
		
		totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest.setOtp(totp3[0]);
				
		ManagementRequest deactivateRequest = new ManagementRequest();
		deactivateRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, deviceRequest3, deviceRequest4});		
		deactivateRequest.setAuthorizationRequest(authorizationRequest);
		
		response = client.deactivate(deactivateRequest);
		assertThat(response.getResponses(), is(notNullValue()));
		assertThat(response.getResponses().length, is(4));
		assertThat(response.getResponses()[0].getMessage(), is("Success"));
		assertThat(response.getResponses()[0].isError(), is(false));		
		assertThat(response.getResponses()[1].getMessage(), is("Error: The device is an installed administrator and cannot be deactivated"));
		assertThat(response.getResponses()[1].isError(), is(true));
		assertThat(response.getResponses()[2].getMessage(), is("Error: Device was not found"));
		assertThat(response.getResponses()[2].isError(), is(true));		
		assertThat(response.getResponses()[3].getMessage(), is("Error: The device testinactivedevice1 cannot be deactivated. It is INACTIVE"));
		assertThat(response.getResponses()[3].isError(), is(true));		

		List<AdministratorResponse> administrators = client.listAdministrators();
		assertThat(administrators.size(), is(4));
		
		CountResponse countResponse = client.countDevices();
		assertThat(countResponse.getCount(), is(10L));
		
		List<DeviceResponse> deviceResponses = client.getDevices(Status.ACTIVE, 3, 1);
		assertThat(deviceResponses.size(),is(1));
		
		deviceResponses = client.getDevices();
		assertThat(deviceResponses.size(), is(10));	
		
		SyncResponse syncResponse = client.synchronize();
		assertThat(syncResponse, is(notNullValue()));
		assertThat((syncResponse.getTime()-System.currentTimeMillis())<2, is(true));
		
		AlternativeConnectionManager.run(incoming->{
			Map<String, String> connectedResponse = new HashMap<>();
			connectedResponse.put("connected", "true");
			try {
				return jwtGenerator.generateJWT(connectedResponse).getBytes();
			} catch (NoSuchAlgorithmException | JWTException | URISyntaxException e) {
				throw new AssertionError(e);
			}
		});
		
		assertThat(client.checkConnection("testdevice1").isConnected(), is(true));
		testInEnhancedModeFalse();
	}

	private Device provisionTestProvisionDevice1() throws IOException, HttpClientException, IllegalCryptoActionException {
		if(device1 == null){
			provisioningEvent = null;
			ProvisioningResponse provisioningResponse = client.provision("testprovisiondevice1");
			assertThat(provisioningResponse.getSeed(), is(notNullValue()));
			assertThat(provisioningResponse.getInitialCounter()>0, is(true));
			
			device1 = new Device();
			device1.setId("testprovisiondevice1");
			device1.setInitialCounter(provisioningResponse.getInitialCounter());
			EncryptedValue encryptedValue = new EncryptedValue();
			encryptedValue.setValue(CryptoResource.hexAsByte(provisioningResponse.getSeed()));
			device1.setSeed(encryptedValue);
			
			String[] totp1 = totpGenerator.generateFor(device1, System.currentTimeMillis());
			
			ActivationRequest activationRequest = new ActivationRequest();
			activationRequest.setToken(totp1[0]);
			activationRequest.setAlias("testActivationAlias1");
			activationRequest.addAttribute("other", "attribute");
			
			client.activate("testprovisiondevice1", activationRequest);
			await().atMost(Duration.ONE_SECOND).untilAsserted(()->{
				assertNotNull(provisioningEvent);
				assertThat(provisioningEvent.getDevice(), is(notNullValue()));
				assertThat(provisioningEvent.getDevice().getId(), is("testprovisiondevice1"));
				assertThat(provisioningEvent.getActivationRequest(), is(notNullValue()));
				assertThat(provisioningEvent.getActivationRequest().getOtherAttributes().get("other"), is("attribute"));
			});
		}
		return device1;
	}

	private Device provisionAdministrator1() throws IOException, HttpClientException, IllegalCryptoActionException {
		if(device2 == null){
			EncryptedValue encryptedValue;
			ActivationRequest activationRequest;
			ProvisioningResponse provisioningResponse = client.provision("testadministrator1");
			assertThat(provisioningResponse.getSeed(), is(notNullValue()));
			assertThat(provisioningResponse.getInitialCounter()>0, is(true));
			
			device2 = new Device();
			device2.setId("testadministrator1");
			device2.setInitialCounter(provisioningResponse.getInitialCounter());
			encryptedValue = new EncryptedValue();
			encryptedValue.setValue(CryptoResource.hexAsByte(provisioningResponse.getSeed()));
			device2.setSeed(encryptedValue);
			
			String[] totp2 = totpGenerator.generateFor(device2, System.currentTimeMillis());
			
			activationRequest = new ActivationRequest();
			activationRequest.setToken(totp2[0]);
			
			client.activate("testadministrator1", activationRequest);
		}
		return device2;
	}

	private Device provisionAdministrator2() throws IOException, HttpClientException, IllegalCryptoActionException {
		if(device3 == null){
			ProvisioningResponse provisioningResponse;
			EncryptedValue encryptedValue;
			ActivationRequest activationRequest;
			provisioningResponse = client.provision("testadministrator2");
			assertThat(provisioningResponse.getSeed(), is(notNullValue()));
			assertThat(provisioningResponse.getInitialCounter()>0, is(true));
			
			device3 = new Device();
			device3.setId("testadministrator2");
			device3.setInitialCounter(provisioningResponse.getInitialCounter());
			encryptedValue = new EncryptedValue();
			encryptedValue.setValue(CryptoResource.hexAsByte(provisioningResponse.getSeed()));
			device3.setSeed(encryptedValue);
			
			String[] totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
			
			activationRequest = new ActivationRequest();
			activationRequest.setToken(totp3[0]);
			
			client.activate("testadministrator2", activationRequest);
		}
		return device3;
	}

	private void testInEnhancedModeFalse() throws Exception {
		ProvisioningResponse provisioningResponse;
		EncryptedValue encryptedValue;
		ActivationRequest activationRequest;
		AlternativeTOTPOptions.returnInEnhancedMode(Boolean.FALSE);
		try {
			provisioningResponse = client.provision("nonenhanced");
			assertThat(provisioningResponse.getSeed(), is(notNullValue()));
			assertThat(provisioningResponse.getInitialCounter(), is(0l));
			
			Device device = new Device();
			device.setId("nonenhanced");
			device.setInitialCounter(0l);
			encryptedValue = new EncryptedValue();
			encryptedValue.setValue(CryptoResource.hexAsByte(provisioningResponse.getSeed()));
			device.setSeed(encryptedValue);
			
			String[] totp = totpGenerator.generateFor(device, System.currentTimeMillis());
			
			activationRequest = new ActivationRequest();
			activationRequest.setToken(totp[0]);
			
			client.activate("nonenhanced", activationRequest);
		} finally {
			AlternativeTOTPOptions.returnInEnhancedMode(null);
		}
	}
	
	@Test
	public void testLoginWithNullDeviceId() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"AuthenticationRequest is required\"}"));
		client.login(new AuthenticationRequest());
	}
	
	@Test
	public void testActivationWithExistingAlias() throws Exception {
		Device device = provisionTestProvisionDevice1();
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 400; {\"message\":\"The alias supplied is not valid\"}"));
		
		String[] totp1 = totpGenerator.generateFor(device, System.currentTimeMillis());

		ActivationRequest activationRequest = new ActivationRequest();
		activationRequest.setToken(totp1[0]);
		activationRequest.setAlias("testActivationAlias1");
		
		client.activate("testprovisiondevice1", activationRequest);
	}

	@Test
	public void testLoginWithNonAdministratorDeviceId() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"Device is not an administrator\"}"));
		AuthenticationRequest authorizationRequest = new AuthenticationRequest();
		authorizationRequest.setDeviceId("nonexistantdevice1");
		client.login(authorizationRequest);
	}
	
	@Test
	public void testLoginWithWrongAdministratorDeviceOtp() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"Authentication failed\"}"));
		AuthenticationRequest authorizationRequest = new AuthenticationRequest();
		authorizationRequest.setDeviceId("testadmindevice1");
		authorizationRequest.setOtp("invalidotp");
		client.login(authorizationRequest);
	}
	
	@Test
	public void testProvisionWithExistingDeviceId() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"The device cannot be provisioned\"}"));
		client.provision("testdevice1");
	}
	
	@Test
	public void testActivateWithNonexistentDeviceId() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 404; {\"message\":\"The device cannot be activated\"}"));
		ActivationRequest activationRequest = new ActivationRequest();
		activationRequest.setToken("123456");
		client.activate("nonexistentdevice", activationRequest);
	}
	
	@Test
	public void testActivateWithActivatedDeviceId() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 404; {\"message\":\"The device cannot be activated\"}"));
		ActivationRequest activationRequest = new ActivationRequest();
		activationRequest.setToken("123456");
		client.activate("testdevice1", activationRequest);
	}

	@Test
	public void testActivateWithInvalidOtp() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"The device cannot be activated\"}"));
		ActivationRequest activationRequest = new ActivationRequest();
		activationRequest.setToken("invalidotp");
		client.activate("testadmindevice1", activationRequest);
	}

	@Test
	public void testLoginAndAuthorizeWithSameDevice() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"Dual administrator authorization is required\"}"));
		Device device2 = provisionAdministrator1();
		
		String[] totp2 = totpGenerator.generateFor(device2, System.currentTimeMillis());
		AuthenticationRequest authorizationRequest = new AuthenticationRequest();
		authorizationRequest.setDeviceId("testadministrator1");
		authorizationRequest.setOtp(totp2[1]==null? totp2[0]:totp2[1]);
				
		client.login(authorizationRequest);
		DeviceRequest deviceRequest1 = new DeviceRequest();
		deviceRequest1.setAlias("testrestassign2");
		deviceRequest1.setDeviceId("testadministrator1");
		
		ManagementRequest assignRequest = new ManagementRequest();
		assignRequest.setAuthorizationRequest(authorizationRequest);
		assignRequest.setRequests(new DeviceRequest[]{deviceRequest1});
		
		client.assignAliases(assignRequest);
	}
	
	@Test
	public void testLoginAndAuthorizeWithNonAdministrator() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"Cannot authorize this request\"}"));
		
		Device device1 = provisionTestProvisionDevice1();
		
		Device device2 = provisionAdministrator1();
		
		String[] totp2 = totpGenerator.generateFor(device2, System.currentTimeMillis());
		AuthenticationRequest authorizationRequest = new AuthenticationRequest();
		authorizationRequest.setDeviceId("testadministrator1");
		authorizationRequest.setOtp(totp2[0]);
		
		client.login(authorizationRequest);
		
		DeviceRequest deviceRequest1 = new DeviceRequest();
		deviceRequest1.setAlias("testrestassign2");
		deviceRequest1.setDeviceId("testadministrator1");
		
		totp2 = totpGenerator.generateFor(device1, System.currentTimeMillis());
		authorizationRequest.setDeviceId("testprovisiondevice1");
		authorizationRequest.setOtp(totp2[0]);
		
		ManagementRequest assignRequest = new ManagementRequest();
		assignRequest.setAuthorizationRequest(authorizationRequest);
		assignRequest.setRequests(new DeviceRequest[]{deviceRequest1});
		
		client.assignAliases(assignRequest);
	}
	
	@Test
	public void testLoginAndAuthorizeWithNoDeviceId() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"AuthenticationRequest is required\"}"));
				
		Device device2 = provisionAdministrator1();
		
		String[] totp2 = totpGenerator.generateFor(device2, System.currentTimeMillis());
		AuthenticationRequest authorizationRequest = new AuthenticationRequest();
		authorizationRequest.setDeviceId("testadministrator1");
		authorizationRequest.setOtp(totp2[0]);
		
		client.login(authorizationRequest);
		
		DeviceRequest deviceRequest1 = new DeviceRequest();
		deviceRequest1.setAlias("testrestassign2");
		deviceRequest1.setDeviceId("testadministrator1");
		
		authorizationRequest.setDeviceId(null);
		authorizationRequest.setOtp(totp2[0]);
		
		ManagementRequest assignRequest = new ManagementRequest();
		assignRequest.setAuthorizationRequest(authorizationRequest);
		assignRequest.setRequests(new DeviceRequest[]{deviceRequest1});
		
		client.assignAliases(assignRequest);
	}
	
	@Test
	public void testAuthorizeManagedRequestInterceptorWithNoSubject() throws Exception {
		expectedException.expect(RestSecurityException.class);
		expectedException.expectMessage(is("Dual administrator authorization is required"));
		
		Device device3 = provisionAdministrator2();
		
		String[] totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());

		AuthenticationRequest authorizationRequest;
		authorizationRequest = new AuthenticationRequest();
		authorizationRequest.setDeviceId("testadministrator2");
		authorizationRequest.setOtp(totp3[0]);
				
		ManagementRequest assignRequest = new ManagementRequest();
		assignRequest.setAuthorizationRequest(authorizationRequest);
		
		new AuthorizeManagedRequestInterceptor().intercept(MockingProxyBuilder
				.createMockingInvocationHandlerFor(InvocationContext.class)
				.mock("getParameters").with(arguments->new Object[]{assignRequest})
				.thenBuild());
	}
	
	@Test
	public void testDataStoreExceptionMapper() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 417; {\"message\":\"DataStoreException\"}"));
		AlternativeDeviceManagementService.throwRuntimeException(new DataStoreException("DataStoreException"));
		
		AuthenticationRequest authenticateRequest = new AuthenticationRequest();
		
		authenticateRequest.setDeviceId("testdevice1");
		authenticateRequest.setOtp("123456");
		
		client.authenticate(authenticateRequest);
	}
	
	@Test
	public void testDataStoreExceptionMapperWithCause() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 417; {\"message\":\"DataStoreExceptionOtherException\"}"));
		AlternativeDeviceManagementService.throwRuntimeException(new DataStoreException("DataStoreException", new Exception("OtherException")));
		
		AuthenticationRequest authenticateRequest = new AuthenticationRequest();
		
		authenticateRequest.setDeviceId("testdevice1");
		authenticateRequest.setOtp("123456");
		
		client.authenticate(authenticateRequest);
	}
	
	@Test
	public void testGeneralExceptionMapper() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 500; {\"message\":\"IllegalArgumentException\"}"));
		AlternativeDeviceManagementService.throwRuntimeException(new IllegalArgumentException("IllegalArgumentException"));
		
		AuthenticationRequest authenticateRequest = new AuthenticationRequest();
		
		authenticateRequest.setDeviceId("testdevice1");
		authenticateRequest.setOtp("123456");
		
		client.authenticate(authenticateRequest);
	}
	
	@Test
	public void testGeneralExceptionMapperWithCause() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 500; {\"message\":\"IllegalArgumentExceptionOtherException\"}"));
		AlternativeDeviceManagementService.throwRuntimeException(new IllegalArgumentException("IllegalArgumentException", new Exception("OtherException")));
		
		AuthenticationRequest authenticateRequest = new AuthenticationRequest();
		
		authenticateRequest.setDeviceId("testdevice1");
		authenticateRequest.setOtp("123456");
		
		client.authenticate(authenticateRequest);
	}
	
	@Test
	public void testMessageGenerationException() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 500; {\"message\":\"Message generation failed. IllegalArgumentException\"}"));
		AlternativeDeviceAuthorizationService.throwException(new MessageGenerationException(new IllegalArgumentException("IllegalArgumentException")));
		DirectAuthenticationRequest request = new DirectAuthenticationRequest();
		request.setDeviceId("testdevice1");
		client.authenticateDirect(request);
	}
		
	@Test
	public void testUnconnectedDeviceExceptionTimeout() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 404; {\"message\":\"The specified device is not connected. Connection timed out\"}"));
		AlternativeDeviceAuthorizationService.doNothing(true);
		DirectAuthenticationRequest request = new DirectAuthenticationRequest();
		request.setDeviceId("testdevice1");
		client.authenticateDirect(request);
	}
	
	@Test
	public void testUnconnectedDeviceExceptionErrorMessage() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 404; {\"message\":\"The specified device is not connected. Error message\"}"));
		AlternativeDeviceAuthorizationService.callErrorCallback(true);
		DirectAuthenticationRequest request = new DirectAuthenticationRequest();
		request.setDeviceId("testdevice1");
		client.authenticateDirect(request);
	}
	
	@Test
	public void testAuthenticateWithInvalidOTP() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"OTP did not match\"}"));		
		AlternativeAuthenticationService.returnAuthenticate((device, otp)->false);
		AuthenticationRequest authenticateRequest = new AuthenticationRequest();
		authenticateRequest.setDeviceId("testdevice1");
		authenticateRequest.setOtp("invalid");
		client.authenticate(authenticateRequest);
	}
		
	@Test
	public void testManagementWithoutAuthorization() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"Authorization failed\"}"));
		Device device = provisionAdministrator2();
		String[] totp = totpGenerator.generateFor(device, System.currentTimeMillis());
		AuthenticationRequest authorizationRequest = new AuthenticationRequest();
		authorizationRequest.setDeviceId("testadministrator2");
		authorizationRequest.setOtp(totp[0]);
		
		DeviceRequest deviceRequest1 = new DeviceRequest();
		deviceRequest1.setAlias("testrestassign1");
		deviceRequest1.setDeviceId("testprovisiondevice1");

		DeviceRequest deviceRequest2 = new DeviceRequest();
		deviceRequest2.setAlias("testrestassignnonexistentdevice1");
		deviceRequest2.setDeviceId("testnonexistentdevice1");

		DeviceRequest deviceRequest3 = new DeviceRequest();
		deviceRequest3.setAlias("testalias1");
		deviceRequest3.setDeviceId("testdevice1");
		
		DeviceRequest deviceRequest4 = new DeviceRequest();
		deviceRequest4.setDeviceId("testdevice1");
		
		ManagementRequest assignRequest = new ManagementRequest();
		assignRequest.setAuthorizationRequest(authorizationRequest);
		assignRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, deviceRequest3, deviceRequest4});
		try {
			client.setRequestJWTToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
			client.assignAliases(assignRequest);
		} finally {
			client.setRequestJWTToken(null);
		}
	}
	
	@Test
	public void testSignRequestWithError() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 404; {\"message\":\"The specified device is not connected. Error message\"}"));
		Device device = provisionTestProvisionDevice1();
		AlternativeConnectionManager.run(bite->{
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("error", "Error message");
			try {
				return jwtGenerator.generateJWT(responseMap).getBytes();
			} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
				throw new AssertionError(e);
			}
		});
		
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("request", "value");
		client.signRequest(device.getId(), requestMap);
	}
	
	@Test
	public void testSignRequestWithException() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 500; {\"message\":\"Message generation failed. Error message 2\"}"));

		Device device = provisionTestProvisionDevice1();
		
		AlternativeDeviceRequestSigningService
			.throwException(new MessageGenerationException(new Exception("Error message 2")));
		
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("request", "value");
		client.signRequest(device.getId(), requestMap);
	}
	
	@Test
	public void testSignRequestWithTimeout() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 404; {\"message\":\"The specified device is not connected. Connection timed out\"}"));
		AlternativeDeviceRequestSigningService.doNothing(true);

		Device device = provisionTestProvisionDevice1();

		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("request", "value");
		client.signRequest(device.getId(), requestMap);
	}
	
	@Test
	public void testVerifySignRequestWithInvalidDevice() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 404; {\"message\":\"Device with ID notFoundDeviceId not found\"}"));
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("request", "value");
		requestMap.put("totp-timestamp", Long.toString(System.currentTimeMillis()));
		client.verifySignedRequest(jwtGenerator.generateJWT(requestMap),"notFoundDeviceId");
	}
	
	@Test
	public void testCheckConnectionWithInvalidDevice() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 404; {\"message\":\"Device with ID notFoundDeviceId not found\"}"));
		Device device2 = provisionAdministrator1();
		
		String[] totp2 = totpGenerator.generateFor(device2, System.currentTimeMillis());

		AuthenticationRequest authorizationRequest = new AuthenticationRequest();
		authorizationRequest.setDeviceId("testadministrator1");
		authorizationRequest.setOtp(totp2[0]);

		client.login(authorizationRequest);
		client.checkConnection("notFoundDeviceId");
	}

	
	@Test
	public void testCheckConnectionWithTOTPException() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 500; {\"message\":\"Message generation failed. Thrown error\"}"));

		AlternativeDeviceManagementService.throwTOTPException(new MessageGenerationException(new Exception("Thrown error")));
		
		Device device2 = provisionAdministrator1();
		String[] totp2 = totpGenerator.generateFor(device2, System.currentTimeMillis());

		AuthenticationRequest authorizationRequest = new AuthenticationRequest();
		authorizationRequest.setDeviceId("testadministrator1");
		authorizationRequest.setOtp(totp2[1] == null? totp2[0]:totp2[1]);

		client.login(authorizationRequest);
		client.checkConnection("testadministrator1");
	}

}
