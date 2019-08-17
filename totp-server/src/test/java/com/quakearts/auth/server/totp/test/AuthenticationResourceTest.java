package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.core.Is.*;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeAuthenticationService;
import com.quakearts.auth.server.totp.alternatives.AlternativeConnectionManager;
import com.quakearts.auth.server.totp.alternatives.AlternativeDeviceService;
import com.quakearts.auth.server.totp.generator.JWTGenerator;
import com.quakearts.auth.server.totp.rest.AuthenticationResource;
import com.quakearts.auth.server.totp.rest.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.rest.model.ErrorResponse;
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
	private JWTGenerator jwtGenerator;

	@Test
	public void testLoginOk() throws Exception {
		Device device = new Device();
		device.setStatus(Status.ACTIVE);
		AlternativeDeviceService.returnDevice(id-> {
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
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(messageIs("AuthenticationRequest is required"));		
		authenticationResource.authenticate(createRequest(null, null));
	}
	
	@Test
	public void testLoginDeviceNotFound() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(messageIs("Device with ID testlogin1 not found"));
		AlternativeDeviceService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.empty();
		});
		
		authenticationResource.authenticate(createRequest("testlogin1", "123456"));
	}
	
	private Matcher<?> messageIs(String string) {
		return new BaseMatcher<WebApplicationException>(){

			@Override
			public boolean matches(Object item) {
				WebApplicationException applicationException = (WebApplicationException) item;
				Object entityObject = applicationException.getResponse().getEntity();
				if(entityObject instanceof ErrorResponse) {
					return string !=null && string.equals(((ErrorResponse)entityObject).getMessage());
				}
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Expected message "+string);
			}};
	}

	@Test
	public void testLoginDeviceINITIATED() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(messageIs("Device with ID testlogin1 not found"));
		Device device = new Device();
		device.setStatus(Status.INITIATED);
		AlternativeDeviceService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		authenticationResource.authenticate(createRequest("testlogin1", "123456"));
	}
	
	@Test
	public void testLoginDeviceINACTIVE() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(messageIs("Device with ID testlogin1 not found"));
		Device device = new Device();
		device.setStatus(Status.INACTIVE);
		AlternativeDeviceService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		authenticationResource.authenticate(createRequest("testlogin1", "123456"));
	}
	
	@Test
	public void testLoginDeviceLOCKED() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(messageIs("Device with ID testlogin1 not found"));
		Device device = new Device();
		device.setStatus(Status.LOCKED);
		AlternativeDeviceService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		authenticationResource.authenticate(createRequest("testlogin1", "123456"));
	}

	@Test
	public void testLoginNotOk() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(messageIs("OTP did not match"));
		Device device = new Device();
		device.setStatus(Status.ACTIVE);
		AlternativeDeviceService.returnDevice(id-> {
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
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(messageIs("OTP did not match"));
		Device device = new Device();
		device.setStatus(Status.ACTIVE);
		AlternativeDeviceService.returnDevice(id-> {
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
		
		AlternativeDeviceService.returnLock(lockDevice->{
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
		AlternativeDeviceService.returnDevice(id-> {
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
		
		authenticationResource.authenticateDirect("testDirect1");
	}
	
	@Test
	public void testAuthenticateDirectDeviceNotFound() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(messageIs("Device with ID testDirect2 not found"));
		AlternativeDeviceService.returnDevice(id-> {
			assertThat(id, is("testDirect2"));
			return Optional.empty();
		});
		authenticationResource.authenticateDirect("testDirect2");
	}
	
	@Test
	public void testAuthenticateDirectDeviceNotActive() throws Exception {
		expectedException.expect(WebApplicationException.class);
		expectedException.expect(messageIs("Device with ID testDirect2 not found"));
		Device device = new Device();
		device.setStatus(Status.INACTIVE);
		AlternativeDeviceService.returnDevice(id-> {
			assertThat(id, is("testDirect2"));
			return Optional.of(device);
		});
		authenticationResource.authenticateDirect("testDirect2");
	}
}
