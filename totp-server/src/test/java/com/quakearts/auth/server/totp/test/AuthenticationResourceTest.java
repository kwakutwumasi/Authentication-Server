package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;
import static org.awaitility.Awaitility.*;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.*;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;

import org.awaitility.Duration;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeAuthenticationService;
import com.quakearts.auth.server.totp.alternatives.AlternativeConnectionManager;
import com.quakearts.auth.server.totp.alternatives.AlternativeDeviceManagementService;
import com.quakearts.auth.server.totp.alternatives.AlternativeTOTPOptions;
import com.quakearts.auth.server.totp.exception.AuthenticationException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.generator.impl.JWTGeneratorImpl;
import com.quakearts.auth.server.totp.rest.AuthenticationResource;
import com.quakearts.auth.server.totp.rest.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.rest.model.DirectAuthenticationRequest;
import com.quakearts.auth.server.totp.rest.model.ErrorResponse;
import com.quakearts.tools.test.mocking.VoidMockedImplementation;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class AuthenticationResourceTest {

	@Inject
	private AuthenticationResource authenticationResource;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Inject
	private JWTGeneratorImpl jwtGenerator;

	@Test
	public void testLoginOk() throws Exception {
		Device device = new Device();
		device.setStatus(Status.ACTIVE);
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		AlternativeAuthenticationService.returnAuthenticate((authDevice,otp)->{
					assertThat(authDevice, is(device));
					assertThat(otp, is("123456"));
					return true;
				});
		
		AlternativeAuthenticationService.returnLocked(checkDevice->{
			assertThat(checkDevice, is(device));
			return false;
		});
		
		authenticationResource.authenticate(createRequest("testlogin1", "123456"));
	}
	
	private AuthenticationRequest createRequest(String deviceId, String otp) {
		AuthenticationRequest request = new AuthenticationRequest();
		request.setDeviceId(deviceId);
		request.setOtp(otp);
		return request;
	}

	@Test
	public void testLoginNoDeviceId() throws Exception {
		expectedException.expect(AuthenticationException.class);
		expectedException.expectMessage("deviceId is required");		
		authenticationResource.authenticate(createRequest(null, null));
	}
	
	@Test
	public void testLoginDeviceNotFound() throws Exception {
		expectedException.expect(AuthenticationException.class);
		expectedException.expectMessage("Device with ID testlogin1 not found");
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.empty();
		});
		
		authenticationResource.authenticate(createRequest("testlogin1", "123456"));
	}

	@Test
	public void testLoginDeviceINITIATED() throws Exception {
		expectedException.expect(AuthenticationException.class);
		expectedException.expectMessage("Device with ID testlogin1 not found");
		Device device = new Device();
		device.setStatus(Status.INITIATED);
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		authenticationResource.authenticate(createRequest("testlogin1", "123456"));
	}
	
	@Test
	public void testLoginDeviceINACTIVE() throws Exception {
		expectedException.expect(AuthenticationException.class);
		expectedException.expectMessage("Device with ID testlogin1 not found");
		Device device = new Device();
		device.setStatus(Status.INACTIVE);
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		authenticationResource.authenticate(createRequest("testlogin1", "123456"));
	}
	
	@Test
	public void testLoginDeviceLOCKED() throws Exception {
		expectedException.expect(AuthenticationException.class);
		expectedException.expectMessage("Device with ID testlogin1 not found");
		Device device = new Device();
		device.setStatus(Status.LOCKED);
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		authenticationResource.authenticate(createRequest("testlogin1", "123456"));
	}

	@Test
	public void testLoginNotOk() throws Exception {
		expectedException.expect(AuthenticationException.class);
		expectedException.expectMessage("OTP did not match");
		Device device = new Device();
		device.setStatus(Status.ACTIVE);
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		AlternativeAuthenticationService.returnAuthenticate((authDevice,otp)->{
					assertThat(authDevice, is(device));
					assertThat(otp, is("123456"));
					return false;
				});
		
		AlternativeAuthenticationService.returnLocked(checkDevice->{
			assertThat(checkDevice, is(device));
			return false;
		});
		
		authenticationResource.authenticate(createRequest("testlogin1", "123456"));
	}
	
	private boolean wasLocked;
	
	@Test
	public void testLoginNotOkAndLocked() throws Exception {
		expectedException.expect(AuthenticationException.class);
		expectedException.expectMessage("OTP did not match");
		Device device = new Device();
		device.setStatus(Status.ACTIVE);
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		AlternativeAuthenticationService.returnAuthenticate((authDevice,otp)->{
					assertThat(authDevice, is(device));
					assertThat(otp, is("123456"));
					return false;
				});
		
		AlternativeAuthenticationService.returnLocked(checkDevice->{
			assertThat(checkDevice, is(device));
			return true;
		});
		
		AlternativeDeviceManagementService.returnLock(lockDevice->{
			assertThat(lockDevice, is(device));
			wasLocked = true;
			return true;
		});
		
		try {
			authenticationResource.authenticate(createRequest("testlogin1", "123456"));			
		} finally {
			assertThat(wasLocked, is(true));
		}
	}
	
	@Test
	public void testAuthenticateDirectLoginOk() throws Exception {
		Device device = new Device();
		device.setStatus(Status.ACTIVE);
		device.setId("testDirect1");
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testDirect1"));
			return Optional.of(device);
		});
		
		AlternativeAuthenticationService.returnAuthenticate((authDevice,otp)->{
			assertThat(authDevice, is(device));
			assertThat(otp, is("123456"));
			return true;
		});
		
		AlternativeAuthenticationService.returnLocked(checkDevice->{
			assertThat(checkDevice, is(device));
			return false;
		});
		
		AlternativeConnectionManager.run(bite->{
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("otp", "123456");
			try {
				return jwtGenerator.generateJWT(responseMap).getBytes();
			} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
				throw new AssertionError(e);
			}
		});
		
		AlternativeTOTPOptions.returnDeviceAuthenticationTimeout(2000l);
		
		class ResponseHolder {
			Object value = new Object();
		}
		
		ResponseHolder response = new ResponseHolder();
		
		DirectAuthenticationRequest authenticationRequest = new DirectAuthenticationRequest();
		authenticationRequest.setDeviceId("testDirect1");
		authenticationRequest.setAuthenticationData(new HashMap<>());
		
		authenticationResource.authenticateDirect(authenticationRequest, 
				mockAsyncResponse(arguments->{
					response.value = arguments.get(0);
				}));
		await().atMost(Duration.ONE_SECOND).until(()->{
				return response.value != null
						&& ((Response) response.value).getStatus() == 204;
			});
		
		assertThat(time, is(2000l));
		assertThat(timeUnit, is(TimeUnit.MILLISECONDS));
		assertThat(handler, is(IsNull.notNullValue()));
	}
	
	@Test
	public void testAuthenticateDirectLoginFailed(){
		try {
			Device device = new Device();
			device.setStatus(Status.ACTIVE);
			device.setId("testDirectFailed1");
			AlternativeDeviceManagementService.returnDevice(id-> {
				return Optional.of(device);
			});
			
			AlternativeAuthenticationService.returnAuthenticate((authDevice,otp)->{
				return false;
			});
			
			AlternativeAuthenticationService.returnLocked(checkDevice->{
				return false;
			});
			
			AlternativeConnectionManager.run(bite->{
				Map<String, String> responseMap = new HashMap<>();
				responseMap.put("otp", "123456");
				try {
					return jwtGenerator.generateJWT(responseMap).getBytes();
				} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
					throw new AssertionError(e);
				}
			});
			
			AlternativeTOTPOptions.returnDeviceAuthenticationTimeout(2000l);
			
			class ResponseHolder {
				Object value;
			}
			
			ResponseHolder response = new ResponseHolder();
			
			DirectAuthenticationRequest authenticationRequest = new DirectAuthenticationRequest();
			authenticationRequest.setDeviceId("testDirectFailed1");
			authenticationRequest.setAuthenticationData(new HashMap<>());

			authenticationResource.authenticateDirect(authenticationRequest, 
					mockAsyncResponse(arguments->{
						response.value = arguments.get(0);
					}));
			await().atMost(Duration.FIVE_SECONDS)
			.until(()->{
				return response.value instanceof AuthenticationException
						&& ((AuthenticationException)response.value)
						.getMessage().equals("OTP did not match");
			});			
		} catch (Exception e) {
			fail("Error thrown: "+e.getLocalizedMessage());
		}
	}
	
	@Test
	public void testAuthenticateDirectLoginRejected(){
		try {
			Device device = new Device();
			device.setStatus(Status.ACTIVE);
			device.setId("testDirectRejected");
			AlternativeDeviceManagementService.returnDevice(id-> {
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
			
			AlternativeTOTPOptions.returnDeviceAuthenticationTimeout(2000l);
			
			class ResponseHolder {
				Object value;
			}
			
			ResponseHolder response = new ResponseHolder();
			
			DirectAuthenticationRequest authenticationRequest = new DirectAuthenticationRequest();
			authenticationRequest.setDeviceId("testDirectRejected");
			authenticationRequest.setAuthenticationData(new HashMap<>());

			authenticationResource.authenticateDirect(authenticationRequest, 
					mockAsyncResponse(arguments->{
						response.value = arguments.get(0);
					}));
			await().atMost(Duration.ONE_SECOND)
			.until(()->{
				return response.value instanceof AuthenticationException
						&& ((AuthenticationException)response.value)
						.getMessage().equals("Request rejected");
			});			
		} catch (Exception e) {
			fail("Error thrown: "+e.getLocalizedMessage());
		}
	}
	
	@Test
	public void testAuthenticateDirectLoginNotConnected(){
		try {
			Device device = new Device();
			device.setStatus(Status.ACTIVE);
			device.setId("testDirectRejected");
			AlternativeDeviceManagementService.returnDevice(id-> {
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
			
			AlternativeTOTPOptions.returnDeviceAuthenticationTimeout(2000l);
			
			class ResponseHolder {
				Object value;
			}
			
			ResponseHolder response = new ResponseHolder();
			
			DirectAuthenticationRequest authenticationRequest = new DirectAuthenticationRequest();
			authenticationRequest.setDeviceId("testDirectRejected");
			authenticationRequest.setAuthenticationData(new HashMap<>());

			authenticationResource.authenticateDirect(authenticationRequest, 
					mockAsyncResponse(arguments->{
						response.value = arguments.get(0);
					}));
			await().atMost(Duration.ONE_SECOND)
			.until(()->{
				return response.value instanceof UnconnectedDeviceException
						&& ((UnconnectedDeviceException)response.value)
						.getMessage().equals("The specified device is not connected. Not connected");
			});			
		} catch (Exception e) {
			fail("Error thrown: "+e.getLocalizedMessage());
		}
	}
	
	@Test
	public void testAuthenticateDirectDeviceNotFound() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(responseMessageIs("Device with ID testDirect2 not found"));
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testDirect2"));
			return Optional.empty();
		});
		DirectAuthenticationRequest authenticationRequest = new DirectAuthenticationRequest();
		authenticationRequest.setDeviceId("testDirect2");
		authenticationRequest.setAuthenticationData(new HashMap<>());

		authenticationResource.authenticateDirect(authenticationRequest, mockAsyncResponse(arguments->{}));
	}
	
	@Test
	public void testAuthenticateDirectDeviceNotActive() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(responseMessageIs("Device with ID testDirect2 not found"));
		Device device = new Device();
		device.setStatus(Status.INACTIVE);
		AlternativeDeviceManagementService.returnDevice(id-> {
			assertThat(id, is("testDirect2"));
			return Optional.of(device);
		});
		DirectAuthenticationRequest authenticationRequest = new DirectAuthenticationRequest();
		authenticationRequest.setDeviceId("testDirect2");
		authenticationRequest.setAuthenticationData(new HashMap<>());

		authenticationResource.authenticateDirect(authenticationRequest, mockAsyncResponse(arguments->{}));
	}
	
	@Test
	public void testAuthenticateDirectDeviceWithNoDeviceId() throws Exception {
		expectedException.expect(AuthenticationException.class);
		expectedException.expectMessage("deviceId is required");
		DirectAuthenticationRequest authenticationRequest = new DirectAuthenticationRequest();
		authenticationRequest.setAuthenticationData(new HashMap<>());

		authenticationResource.authenticateDirect(authenticationRequest, mockAsyncResponse(arguments->{}));
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

	private TimeoutHandler handler;
	private long time;
	private TimeUnit timeUnit;
	
	public AsyncResponse mockAsyncResponse(VoidMockedImplementation implementation) {
		return MockingProxyBuilder
				.createMockingInvocationHandlerFor(AsyncResponse.class)
				.mock("resume").withVoidMethod(implementation)
				.mock("setTimeout")
				.with(arguments->{
					time = arguments.get(0);
					timeUnit = arguments.get(1);
					return true;
				})
				.mock("setTimeoutHandler")
				.withVoidMethod(arguments->{
					handler = arguments.get(0);
				})
				.thenBuild();
	}
}
