package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeTOTPGenerator;
import com.quakearts.auth.server.totp.authentication.impl.AuthenticationServiceImpl;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.security.cryptography.jpa.EncryptedValue;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class AuthenticationServiceImplTest {	
	@Inject
	private AuthenticationServiceImpl authenticationService;

	@Inject
	private TOTPOptions totpOptions;

	@After
	public void tearDown() throws Exception {}

	@Test
	public void testAuthenticate() throws Exception {
		AlternativeTOTPGenerator.simulate(true);
		Device device = generateDevice();
		assertThat(authenticationService.authenticate(device, "123456"), is(true));
		device.setId("generatetwo");
		AlternativeTOTPGenerator.simulate(true);
		assertThat(authenticationService.authenticate(device, "789101"), is(true));
		assertThat(authenticationService.authenticate(device, ""), is(false));
		for(int i=0;i<totpOptions.getMaxAttempts();i++){
			authenticationService.authenticate(device, "");
		}
		assertThat(authenticationService.isLocked(device), is(true));
		AlternativeTOTPGenerator.simulate(true);
		assertThat(authenticationService.authenticate(device, "789101"), is(false));
	}

	private Device generateDevice() {
		Device device = new Device();
		device.setId("generateone");
		EncryptedValue value = new EncryptedValue();
		value.setDataStoreName(totpOptions.getDataStoreName());
		value.setValue("test".getBytes());
		device.setSeed(value);
		device.setStatus(Status.ACTIVE);
		return device;
	}

}
