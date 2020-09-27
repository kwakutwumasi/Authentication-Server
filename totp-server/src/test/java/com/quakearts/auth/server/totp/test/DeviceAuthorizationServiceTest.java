package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.*;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeConnectionManager;
import com.quakearts.auth.server.totp.generator.impl.JWTGeneratorImpl;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.rest.authorization.impl.DeviceAuthorizationServiceImpl;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webtools.test.AllServicesRunner;

import junit.framework.AssertionFailedError;

@RunWith(AllServicesRunner.class)
public class DeviceAuthorizationServiceTest {

	@Inject
	private DeviceAuthorizationServiceImpl deviceAuthorizationService;
	
	@Inject
	private JWTGeneratorImpl jwtGenerator;
	
	@Test
	public void testSendMessage() throws Exception {
		AlternativeConnectionManager.run(this::expectedMessageAndResponse);
		Device device = new Device();
		device.setId("123456");
		device.setItemCount(1);
		
		deviceAuthorizationService.requestOTPCode(device, null, otp->{			
			assertThat(otp, is("7890"));
		}, error->{});
		HashMap<String, String> authenticationData = new HashMap<>();
		deviceAuthorizationService.requestOTPCode(device, authenticationData, otp->{			
			assertThat(otp, is("7890"));
		}, error->{});
		authenticationData.put("key", "value");
		AlternativeConnectionManager.run(this::expectedMessageAndResponseWithExtraData);
		deviceAuthorizationService.requestOTPCode(device, authenticationData, otp->{			
			assertThat(otp, is("7890"));
		}, error->{});
		AlternativeConnectionManager.run(this::errorMessage);
		deviceAuthorizationService.requestOTPCode(device, authenticationData, otp->{
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
	
	private byte[] expectedMessageAndResponseWithExtraData(byte[] message){
		try {
			JWTClaims jwtClaims = jwtGenerator.verifyJWT(message);
			assertThat(jwtClaims.getPrivateClaim("requestType"), is("otp"));
			assertThat(jwtClaims.getPrivateClaim("deviceId"), is("123456"));
			assertThat(jwtClaims.getPrivateClaim("key"), is("value"));
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
