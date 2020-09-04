package com.quakearts.auth.server.totp.test;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;
import static org.awaitility.Awaitility.*;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;

import org.awaitility.Duration;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeConnectionManager;
import com.quakearts.auth.server.totp.alternatives.AlternativeDeviceManagementService;
import com.quakearts.auth.server.totp.exception.AuthenticationException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.generator.impl.JWTGeneratorImpl;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.rest.RequestSigningResource;
import com.quakearts.auth.server.totp.rest.model.ErrorResponse;
import com.quakearts.tools.test.mocking.MockedImplementation;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class RequestSigningResourceTest {

	@Inject
	private RequestSigningResource requestSigningResource;
		
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Inject
	private JWTGeneratorImpl jwtGenerator;

	@Test
	public void testSigningRequestRejected() throws Exception {
		Device device = new Device();
		device.setId("testDeviceRejected");
		device.setStatus(Status.ACTIVE);
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testDeviceRejected"));
			return Optional.of(device);
		});
		
		AlternativeConnectionManager.run(bite->{
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("error", "Request rejected");
			try {
				return jwtGenerator.generateJWT(responseMap).getBytes();
			} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
				throw new AssertionError(e);
			}
		});
		
		class ResponseHolder {
			Object value = new Object();
		}
		
		ResponseHolder response = new ResponseHolder();
		
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("test", "request");
		requestSigningResource.signRequest("testDeviceRejected", 
			mockAsyncResponse(arguments->{
				response.value = arguments.get(0);
				return true;
			}), requestMap);
		
		await().atMost(Duration.ONE_SECOND).until(()->{
			return response.value instanceof AuthenticationException
					&& ((AuthenticationException)response.value)
					.getMessage().equals("Request rejected");
		});
	}
	
	@Test
	public void testSigningUnconnectedDevice() throws Exception {
		Device device = new Device();
		device.setId("testNotConnected");
		device.setStatus(Status.ACTIVE);
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testNotConnected"));
			return Optional.of(device);
		});
		
		AlternativeConnectionManager.run(bite->{
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("error", "Not connected");
			try {
				return jwtGenerator.generateJWT(responseMap).getBytes();
			} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
				throw new AssertionError(e);
			}
		});
		
		class ResponseHolder {
			Object value = new Object();
		}
		
		ResponseHolder response = new ResponseHolder();
		
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("test", "request");
		requestSigningResource.signRequest("testNotConnected", 
			mockAsyncResponse(arguments->{
				response.value = arguments.get(0);
				return true;
			}), requestMap);
		
		await().atMost(Duration.ONE_SECOND).until(()->{
			return response.value instanceof UnconnectedDeviceException
					&& ((UnconnectedDeviceException)response.value)
					.getMessage().equals("The specified device is not connected. Not connected");
		});
	}
	
	@Test
	public void testSignRequestWithDeviceMissing() {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(responseMessageIs("Device with ID testDirect2 not found"));
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testDirect2"));
			return Optional.empty();
		});
		
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("test", "request");
		requestSigningResource.signRequest("testDirect2", mockAsyncResponse(arguments->true), requestMap);
	}

	@Test
	public void testSignRequestWithUnInitiatedDevice() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(responseMessageIs("Device with ID testDirect2 not found"));
		Device device = new Device();
		device.setStatus(Status.INITIATED);
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testDirect2"));
			return Optional.of(device);
		});
		
		
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("test", "request");
		requestSigningResource.signRequest("testDirect2", mockAsyncResponse(arguments->true), requestMap);
	}
	
	@Test
	public void testSignRequestWithInactiveDevice() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(responseMessageIs("Device with ID testDirect2 not found"));
		Device device = new Device();
		device.setStatus(Status.INACTIVE);
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testDirect2"));
			return Optional.of(device);
		});	
		
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("test", "request");
		requestSigningResource.signRequest("testDirect2", mockAsyncResponse(arguments->true), requestMap);
	}
	
	@Test
	public void testSignRequestWithNoRequestMap() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(responseMessageIs("Request map is required"));
		
		requestSigningResource.signRequest("testDirect2", mockAsyncResponse(arguments->true), null);
	}
	
	@Test
	public void testSignRequestWithEmptyRequestMap() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(responseMessageIs("Request map is required"));
		
		requestSigningResource.signRequest("testDirect2", mockAsyncResponse(arguments->true), new HashMap<>());
	}
		
	private Matcher<?> responseMessageIs(String string) {
		return new BaseMatcher<Throwable>() {
			@Override
			public boolean matches(Object item) {
				if(item instanceof WebApplicationException) {
					return string.equals(((ErrorResponse)((WebApplicationException)item).getResponse()
						.getEntity()).getMessage());
				}
				return false;
			}
			
			@Override
			public void describeTo(Description description) {}
		};
	}

	public AsyncResponse mockAsyncResponse(MockedImplementation implementation) {
		return MockingProxyBuilder
				.createMockingInvocationHandlerFor(AsyncResponse.class)
				.mock("resume").with(implementation)
				.mock("setTimeout")
				.with(arguments->{
					return true;
				})
				.mock("setTimeoutHandler")
				.withVoidMethod(arguments->{})
				.thenBuild();
	}
}
