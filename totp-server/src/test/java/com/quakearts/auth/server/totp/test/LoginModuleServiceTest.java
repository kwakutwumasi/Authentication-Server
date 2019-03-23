package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;

import java.util.Optional;

import static org.hamcrest.core.Is.*;
import javax.inject.Inject;
import javax.security.auth.login.LoginException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeAuthenticationService;
import com.quakearts.auth.server.totp.alternatives.AlternativeDeviceService;
import com.quakearts.auth.server.totp.loginmodule.LoginModuleService;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class LoginModuleServiceTest {

	@Inject
	private LoginModuleService loginModuleService;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
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
		
		loginModuleService.login("testlogin1", "123456".toCharArray());
	}
	
	@Test
	public void testLoginDeviceNotFound() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage(is("Device with ID testlogin1 not found"));
		AlternativeDeviceService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.empty();
		});
		
		loginModuleService.login("testlogin1", "123456".toCharArray());
	}
	
	@Test
	public void testLoginDeviceINITIATED() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage(is("Device with ID testlogin1 not found"));
		Device device = new Device();
		device.setStatus(Status.INITIATED);
		AlternativeDeviceService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		loginModuleService.login("testlogin1", "123456".toCharArray());
	}
	
	@Test
	public void testLoginDeviceINACTIVE() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage(is("Device with ID testlogin1 not found"));
		Device device = new Device();
		device.setStatus(Status.INACTIVE);
		AlternativeDeviceService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		loginModuleService.login("testlogin1", "123456".toCharArray());
	}
	
	@Test
	public void testLoginDeviceLOCKED() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage(is("Device with ID testlogin1 not found"));
		Device device = new Device();
		device.setStatus(Status.LOCKED);
		AlternativeDeviceService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		loginModuleService.login("testlogin1", "123456".toCharArray());
	}

	@Test
	public void testLoginNotOk() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage(is("OTP did not match"));
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
		
		loginModuleService.login("testlogin1", "123456".toCharArray());
	}
	
	private boolean wasLocked;
	
	@Test
	public void testLoginNotOkAndLocked() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage(is("OTP did not match"));
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
			loginModuleService.login("testlogin1", "123456".toCharArray());			
		} finally {
			assertThat(wasLocked, is(true));
		}
	}
}
