package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.*;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeConnectionManager;
import com.quakearts.auth.server.totp.generator.JWTGenerator;
import com.quakearts.auth.server.totp.rest.authorization.DeviceAuthorizationService;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webtools.test.AllServicesRunner;

import junit.framework.AssertionFailedError;

@RunWith(AllServicesRunner.class)
public class DeviceAuthorizationServiceTest {

	@Inject
	private DeviceAuthorizationService deviceConnectionService;
	
	@Inject
	private JWTGenerator jwtGenerator;
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void testSendMessage() throws Exception {
		AlternativeConnectionManager.run(this::expectedMessageAndResponse);
		deviceConnectionService.requestOTPCode("123456",otp->{			
			assertThat(otp, is("7890"));
		}, error->{});
		AlternativeConnectionManager.run(this::errorMessage);
		deviceConnectionService.requestOTPCode("123456", otp->{
			throw new AssertionFailedError("Error callback was not called");
		}, error->{
			assertThat(error, is("Message"));
		});
	}

	private byte[] expectedMessageAndResponse(byte[] message){
		try {
			JWTClaims jwtClaims = jwtGenerator.verifyJWT(message);
			assertThat(jwtClaims.getPrivateClaim("requestType"), is("otp"));
			assertThat(jwtClaims.getPrivateClaim("deviceId"), is("123456"));
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("otp", "7890");
			return jwtGenerator.generateJWT(responseMap).getBytes();			
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
	
	private byte[] errorMessage(byte[] message){
		try {
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("error", "Message");
			return jwtGenerator.generateJWT(responseMap).getBytes();			
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
}
