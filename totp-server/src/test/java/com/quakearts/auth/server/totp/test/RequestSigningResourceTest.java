package com.quakearts.auth.server.totp.test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeDeviceManagementService;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.rest.RequestSigningResource;
import com.quakearts.auth.server.totp.rest.model.ErrorResponse;
import com.quakearts.tools.test.mocking.VoidMockedImplementation;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class RequestSigningResourceTest {

	@Inject
	private RequestSigningResource requestSigningResource;
		
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
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
		requestSigningResource.signRequest("testDirect2", mockAsyncResponse(arguments->{}), requestMap);
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
		requestSigningResource.signRequest("testDirect2", mockAsyncResponse(arguments->{}), requestMap);
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
		requestSigningResource.signRequest("testDirect2", mockAsyncResponse(arguments->{}), requestMap);
	}
	
	@Test
	public void testSignRequestWithNoRequestMap() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(responseMessageIs("Request map is required"));
		
		requestSigningResource.signRequest("testDirect2", mockAsyncResponse(arguments->{}), null);
	}
	
	@Test
	public void testSignRequestWithEmptyRequestMap() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(responseMessageIs("Request map is required"));
		
		requestSigningResource.signRequest("testDirect2", mockAsyncResponse(arguments->{}), new HashMap<>());
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

	public AsyncResponse mockAsyncResponse(VoidMockedImplementation implementation) {
		return MockingProxyBuilder
				.createMockingInvocationHandlerFor(AsyncResponse.class)
				.mock("resume").withVoidMethod(implementation)
				.mock("setTimeout")
				.with(arguments->{
					return true;
				})
				.mock("setTimeoutHandler")
				.withVoidMethod(arguments->{})
				.thenBuild();
	}
}
