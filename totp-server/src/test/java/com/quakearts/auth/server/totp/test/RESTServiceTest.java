package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;

import org.junit.BeforeClass;
import org.junit.Rule;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.generator.TOTPGenerator;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.rest.authorization.AuthorizeManagedRequestInterceptor;
import com.quakearts.auth.server.totp.rest.model.ActivationRequest;
import com.quakearts.auth.server.totp.rest.model.AdministratorResponse;
import com.quakearts.auth.server.totp.rest.model.AuthorizationRequest;
import com.quakearts.auth.server.totp.rest.model.CountResponse;
import com.quakearts.auth.server.totp.rest.model.DeviceRequest;
import com.quakearts.auth.server.totp.rest.model.DeviceResponse;
import com.quakearts.auth.server.totp.rest.model.ManagementRequest;
import com.quakearts.auth.server.totp.rest.model.ManagementResponse;
import com.quakearts.auth.server.totp.rest.model.ProvisioningResponse;
import com.quakearts.auth.server.totp.rest.model.TokenResponse;
import com.quakearts.auth.server.totp.resttest.RESTTestClient;
import com.quakearts.auth.server.totp.resttest.RESTTestClient.SyncResponse;
import com.quakearts.auth.server.totp.resttest.RESTTestClientBuilder;
import com.quakearts.auth.server.totp.setup.CreatorService;
import com.quakearts.rest.client.exception.HttpClientException;
import com.quakearts.security.cryptography.CryptoResource;
import com.quakearts.security.cryptography.exception.IllegalCryptoActionException;
import com.quakearts.security.cryptography.jpa.EncryptedValue;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;
import com.quakearts.webapp.security.rest.exception.RestSecurityException;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class RESTServiceTest {
	
	@BeforeClass
	public static void createDatabase(){
		CreatorService creatorService = CDI.current().select(CreatorService.class).get();
		creatorService.dropAndCreateDatabase();
		creatorService.createEntitiesForTest();
		client = new RESTTestClientBuilder()
				.setURLAs("http://localhost:8080")
				.thenBuild();
	}
	
	@Inject
	private TOTPGenerator totpGenerator;
	private static RESTTestClient client;
	private static Device device2;
	private static Device device3;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	private static Device device1;
		
	@Test
	public void run200OkRequests() throws Exception {
		provisionTestProvisionDevice1();
		
		Device device2 = provisionAdministrator1();
		Device device3 = provisionAdministrator2();
		
		String[] totp2 = totpGenerator.generateFor(device2, System.currentTimeMillis());

		AuthorizationRequest authorizationRequest = new AuthorizationRequest();
		authorizationRequest.setDeviceId("testadministrator1");
		authorizationRequest.setOtp(totp2[0]);
				
		TokenResponse tokenResponse = client.login(authorizationRequest);
		assertThat(tokenResponse.getToken(), is(notNullValue()));
		
		String[] totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest = new AuthorizationRequest();
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
		
		ManagementRequest assignRequest = new ManagementRequest();
		assignRequest.setAuthorizationRequest(authorizationRequest);
		assignRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, deviceRequest3});
		
		ManagementResponse response = client.assignAliases(assignRequest);
		assertThat(response.getResponse(), is(notNullValue()));
		assertThat(response.getResponse().length, is(3));
		assertThat(response.getResponse()[0], is("Success"));
		assertThat(response.getResponse()[1], is("Error: Device was not found"));
		assertThat(response.getResponse()[2], is("Error: The alias testalias1 has already been assigned"));
		
		deviceRequest1.setDeviceId(null);
		deviceRequest2.setDeviceId(null);
		
		totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest.setOtp(totp3[0]);

		ManagementRequest unassignRequest = new ManagementRequest();
		unassignRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2});		
		unassignRequest.setAuthorizationRequest(authorizationRequest);
		
		response = client.unassignAliases(unassignRequest);
		assertThat(response.getResponse(), is(notNullValue()));
		assertThat(response.getResponse().length, is(2));
		assertThat(response.getResponse()[0], is("Success"));
		assertThat(response.getResponse()[1], is("Error: The alias testrestassignnonexistentdevice1 was not assigned"));

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
		assertThat(response.getResponse(), is(notNullValue()));
		assertThat(response.getResponse().length, is(3));
		assertThat(response.getResponse()[0], is("Success"));
		assertThat(response.getResponse()[1], is("Error: The device testinactivedevice1 was not locked"));
		assertThat(response.getResponse()[2], is("Error: Device was not found"));

		totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest.setOtp(totp3[0]);
		ManagementRequest unlockRequest = new ManagementRequest();
		unlockRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, deviceRequest3});		
		unlockRequest.setAuthorizationRequest(authorizationRequest);
		
		response = client.unlock(unlockRequest);
		assertThat(response.getResponse(), is(notNullValue()));
		assertThat(response.getResponse().length, is(3));
		assertThat(response.getResponse()[0], is("Success"));
		assertThat(response.getResponse()[1], is("Error: The device testinactivedevice1 cannot be unlocked. It is INACTIVE"));
		assertThat(response.getResponse()[2], is("Error: Device was not found"));

		totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest.setOtp(totp3[0]);
		
		deviceRequest1.setDeviceId("testprovisiondevice1");
		deviceRequest1.setAlias("Administrator 3");
		deviceRequest2.setDeviceId("testadministrator2");
		deviceRequest2.setAlias("Administrator 4");
		deviceRequest3.setAlias("Administrator 5");
		
		DeviceRequest deviceRequest4 = new DeviceRequest();
		deviceRequest4.setDeviceId("testinactivedevice1");

		DeviceRequest deviceRequest5 = new DeviceRequest();
		deviceRequest5.setAlias("Administrator 6");
		deviceRequest5.setDeviceId("testinactivedevice1");
		
		ManagementRequest addAsAdminRequest = new ManagementRequest();
		addAsAdminRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, 
				deviceRequest3, deviceRequest4, deviceRequest5});		
		addAsAdminRequest.setAuthorizationRequest(authorizationRequest);
		
		response = client.addAsAdmin(addAsAdminRequest);
		assertThat(response.getResponse(), is(notNullValue()));
		assertThat(response.getResponse().length, is(5));
		assertThat(response.getResponse()[0], is("Success"));
		assertThat(response.getResponse()[1], is("Error: The device testadministrator2 is already an administrator device"));
		assertThat(response.getResponse()[2], is("Error: Device was not found"));
		assertThat(response.getResponse()[3], is("Error: Common name was missing"));
		assertThat(response.getResponse()[4], is("Error: The device is not in a valid state. Status is INACTIVE"));

		totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest.setOtp(totp3[0]);
		
		deviceRequest1.setAlias(null);
		deviceRequest2.setDeviceId("testinactivedevice1");
		deviceRequest2.setAlias(null);
		deviceRequest3.setAlias(null);
		
		ManagementRequest removeAsAdminRequest = new ManagementRequest();
		removeAsAdminRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, deviceRequest3});		
		removeAsAdminRequest.setAuthorizationRequest(authorizationRequest);
		
		response = client.removeAsAdmin(removeAsAdminRequest);
		assertThat(response.getResponse(), is(notNullValue()));
		assertThat(response.getResponse().length, is(3));
		assertThat(response.getResponse()[0], is("Success"));
		assertThat(response.getResponse()[1], is("Error: The device testinactivedevice1 is not an administrator device"));
		assertThat(response.getResponse()[2], is("Error: Device was not found"));
		
		totp3 = totpGenerator.generateFor(device3, System.currentTimeMillis());
		authorizationRequest.setOtp(totp3[0]);
				
		ManagementRequest deactivateRequest = new ManagementRequest();
		deactivateRequest.setRequests(new DeviceRequest[]{deviceRequest1, deviceRequest2, deviceRequest3});		
		deactivateRequest.setAuthorizationRequest(authorizationRequest);
		
		response = client.deactivate(deactivateRequest);
		assertThat(response.getResponse(), is(notNullValue()));
		assertThat(response.getResponse().length, is(3));
		assertThat(response.getResponse()[0], is("Success"));
		assertThat(response.getResponse()[1], is("Error: The device testinactivedevice1 cannot be deactivated. It is INACTIVE"));
		assertThat(response.getResponse()[2], is("Error: Device was not found"));

		List<AdministratorResponse> administrators = client.listAdministrators();
		assertThat(administrators.size(), is(4));
		
		CountResponse countResponse = client.countDevices();
		assertThat(countResponse.getCount(), is(11l));
		
		List<DeviceResponse> deviceResponses = client.getDevices(Status.ACTIVE, 3, 1);
		assertThat(deviceResponses.size(),is(1));
		
		deviceResponses = client.getDevices();
		assertThat(deviceResponses.size(),is(11));	
		
		SyncResponse syncResponse = client.synchronize();
		assertThat(syncResponse, is(notNullValue()));
		assertThat((syncResponse.getTime()-System.currentTimeMillis())<2, is(true));
	}

	private Device provisionTestProvisionDevice1() throws IOException, HttpClientException, IllegalCryptoActionException {
		if(device1 == null){
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
			
			client.activate("testprovisiondevice1", activationRequest);
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

	@Test
	public void testLoginWithNullDeviceId() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"AuthorizationRequest is required\"}"));
		client.login(new AuthorizationRequest());
	}

	@Test
	public void testLoginWithNonAdministratorDeviceId() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"Device is not an administrator\"}"));
		AuthorizationRequest authorizationRequest = new AuthorizationRequest();
		authorizationRequest.setDeviceId("nonexistantdevice1");
		client.login(authorizationRequest);
	}
	
	@Test
	public void testLoginWithWrongAdministratorDeviceOtp() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"Authentication failed\"}"));
		AuthorizationRequest authorizationRequest = new AuthorizationRequest();
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
		AuthorizationRequest authorizationRequest = new AuthorizationRequest();
		authorizationRequest.setDeviceId("testadministrator1");
		authorizationRequest.setOtp(totp2[0]);
				
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
		AuthorizationRequest authorizationRequest = new AuthorizationRequest();
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
		expectedException.expectMessage(is("Unable to process request: 403; {\"message\":\"AuthorizationRequest is required\"}"));
				
		Device device2 = provisionAdministrator1();
		
		String[] totp2 = totpGenerator.generateFor(device2, System.currentTimeMillis());
		AuthorizationRequest authorizationRequest = new AuthorizationRequest();
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

		AuthorizationRequest authorizationRequest;
		authorizationRequest = new AuthorizationRequest();
		authorizationRequest.setDeviceId("testadministrator2");
		authorizationRequest.setOtp(totp3[0]);
				
		ManagementRequest assignRequest = new ManagementRequest();
		assignRequest.setAuthorizationRequest(authorizationRequest);
		
		new AuthorizeManagedRequestInterceptor().intercept(MockingProxyBuilder
				.createMockingInvocationHandlerFor(InvocationContext.class)
				.mock("getParameters").with(arguments->new Object[]{assignRequest})
				.thenBuild());
	}
}
