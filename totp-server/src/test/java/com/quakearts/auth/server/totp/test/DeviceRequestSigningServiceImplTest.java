package com.quakearts.auth.server.totp.test;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.appbase.cdi.annotation.Transactional;
import com.quakearts.appbase.cdi.annotation.Transactional.TransactionType;
import com.quakearts.auth.server.totp.alternatives.AlternativeConnectionManager;
import com.quakearts.auth.server.totp.device.impl.DeviceManagementServiceImpl;
import com.quakearts.auth.server.totp.exception.InvalidSignatureException;
import com.quakearts.auth.server.totp.generator.TOTPGenerator;
import com.quakearts.auth.server.totp.generator.impl.JWTGeneratorImpl;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.signing.impl.DeviceRequestSigningServiceImpl;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.JWTHeader;
import com.quakearts.webapp.security.jwt.JWTSigner;
import com.quakearts.webapp.security.jwt.JWTVerifier;
import com.quakearts.webapp.security.jwt.JWTClaims.Claim;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.webapp.security.jwt.factory.JWTFactory;
import com.quakearts.webtools.test.AllServicesRunner;

import junit.framework.AssertionFailedError;

@RunWith(AllServicesRunner.class)
public class DeviceRequestSigningServiceImplTest {

	@Inject
	private JWTGeneratorImpl jwtGenerator;	
	@Inject
	private DeviceRequestSigningServiceImpl deviceRequestSigningService;
	
	@Test
	public void testSignRequest() throws Exception {
		AlternativeConnectionManager.run(this::expectedMessageAndResponse);
		Map<String, String> message = new HashMap<>();
		message.put("request", "sensitive");
		deviceRequestSigningService.signRequest("123456", message, signedMessage->{			
			JWTFactory factory = JWTFactory.getInstance();
			Map<String, String> options = new HashMap<>();
			options.put("secret", "7890");
			JWTVerifier verifier;
			try {
				verifier = factory.getVerifier("HS256", options);
				verifier.verify(signedMessage);
				assertThat(verifier.getClaims().getPrivateClaim("request"), is("sensitive"));
				assertThat(verifier.getClaims().getPrivateClaim("totp-timestamp"), is("12345678901234"));
				for(Claim claim : verifier.getClaims()){
					if(claim.getName().equals("requestType") || 
							claim.getName().equals("deviceId"))
						fail("Contains restricted data");
				}
			} catch (JWTException e) {
				throw new AssertionFailedError(e.getMessage());
			}
		}, error->{
			throw new AssertionFailedError("Error callback called");
		});
	}

	@Test
	public void testSignRequestWithNoOtp() throws Exception {
		AlternativeConnectionManager.run(this::missingOtp);
		Map<String, String> message = new HashMap<>();
		message.put("request", "sensitive");
		deviceRequestSigningService.signRequest("123456", message, signedMessage->{			
			throw new AssertionFailedError("Callback should not be called");
		}, error->{
			assertThat(error, is("Error message"));
		});
	}
	
	@Test
	public void testSignRequestWithNoTimestamp() throws Exception {
		AlternativeConnectionManager.run(this::missingTimestamp);
		Map<String, String> message = new HashMap<>();
		message.put("request", "sensitive");
		deviceRequestSigningService.signRequest("123456", message, signedMessage->{			
			throw new AssertionFailedError("Callback should not be called");
		}, error->{
			assertThat(error, is("Error message 2"));
		});
	}
	
	private byte[] expectedMessageAndResponse(byte[] message){
		try {
			JWTClaims jwtClaims = jwtGenerator.verifyJWT(message);
			assertThat(jwtClaims.getPrivateClaim("requestType"), is("otp-signing"));
			assertThat(jwtClaims.getPrivateClaim("deviceId"), is("123456"));
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("otp", "7890");
			responseMap.put("totp-timestamp", "12345678901234");
			return jwtGenerator.generateJWT(responseMap).getBytes();			
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
	
	private byte[] missingOtp(byte[] message){
		try {
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("totp-timestamp", "12345678901234");
			responseMap.put("error", "Error message");
			return jwtGenerator.generateJWT(responseMap).getBytes();			
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
	
	private byte[] missingTimestamp(byte[] message){
		try {
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("otp", "7890");
			responseMap.put("error", "Error message 2");
			return jwtGenerator.generateJWT(responseMap).getBytes();			
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
	
	@Inject
	private DeviceManagementServiceImpl deviceManagementService;
	
	@Inject
	private TOTPGenerator totpGenerator;

	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testRequestSigning() throws Exception {
		Optional<Device> optionalDevice = deviceManagementService
				.findDevice("testdevice1");
		
		Device device = optionalDevice.get();
		
		long now = System.currentTimeMillis();
		
		String[] totp = totpGenerator.generateFor(device, now);
		JWTFactory factory = JWTFactory.getInstance();
		
		JWTHeader header = factory.createEmptyClaimsHeader();
		JWTClaims claims = factory.createEmptyClaims();
		
		claims.addPrivateClaim("request", "value");
		claims.addPrivateClaim("totp-timestamp", now+"");
		
		Map<String, String> options = new HashMap<>();
		options.put("secret", totp[0]);
		
		JWTSigner signer = factory.getSigner("HS256", options);
		
		String signature = signer.sign(header, claims);
		
		deviceRequestSigningService.verifySignedRequest(device, signature);
	}
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testRequestSigningWithNullSignedRequest() throws Exception {
		expectedException.expect(InvalidSignatureException.class);
		expectedException.expectMessage("signedRequest is required");
		deviceRequestSigningService.verifySignedRequest(null, null);
	}
	
	@Test
	public void testRequestSigningWithEmptySignedRequest() throws Exception {
		expectedException.expect(InvalidSignatureException.class);
		expectedException.expectMessage("signedRequest is required");
		deviceRequestSigningService.verifySignedRequest(null, "  ");
	}

	@Test
	public void testRequestSigningWithInvalidSignedRequest() throws Exception {
		expectedException.expect(InvalidSignatureException.class);
		expectedException.expectMessage("signedRequest must be a valid JWT token with a header, "
				+ "body and signed trailer separated by '.'");
		deviceRequestSigningService.verifySignedRequest(null, "invalid");
	}
	
	@Test
	public void testRequestSigningWithIncompleteSignedRequest() throws Exception {
		expectedException.expect(InvalidSignatureException.class);
		expectedException.expectMessage("signedRequest must be a valid JWT token with a header, "
				+ "body and signed trailer separated by '.'");
		deviceRequestSigningService
			.verifySignedRequest(null,
					"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
				  + "eyJ0ZXN0IjoidmFsdWUiLCJ0b3RwLXRpbWVzd"
				  + "GFtcCI6IjE1MTYyMzkwMjIiLCJpYXQiOjE1MT"
				  + "YyMzkwMjJ9");
	}
	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testRequestSigningWithForgedSignedRequest() throws Exception {
		expectedException.expect(InvalidSignatureException.class);
		expectedException.expectMessage("Unable to verify token: Invalid signature");
		deviceRequestSigningService
			.verifySignedRequest(deviceManagementService
					.findDevice("testdevice1").get(), 
				  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
				+ "eyJ0ZXN0IjoidmFsdWUiLCJ0b3RwLXRpbWVzd"
				+ "GFtcCI6IjE1MTYyMzkwMjIiLCJpYXQiOjE1MT"
				+ "YyMzkwMjJ9.GiakqRGw1DG57uIJLi-cIDoKaE"
				+ "bHM3NeRnyfZoiEpE8");
	}
	
}
