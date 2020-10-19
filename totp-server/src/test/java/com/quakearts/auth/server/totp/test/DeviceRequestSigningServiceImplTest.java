package com.quakearts.auth.server.totp.test;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
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
import com.quakearts.auth.server.totp.alternatives.AlternativeTOTPGenerator;
import com.quakearts.auth.server.totp.device.impl.DeviceManagementServiceImpl;
import com.quakearts.auth.server.totp.exception.InvalidSignatureException;
import com.quakearts.auth.server.totp.generator.impl.JWTGeneratorImpl;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.options.TOTPLoginConfiguration;
import com.quakearts.auth.server.totp.runner.TOTPDatabaseServiceRunner;
import com.quakearts.auth.server.totp.signing.impl.DeviceRequestSigningServiceImpl;
import com.quakearts.security.cryptography.jpa.EncryptedValue;
import com.quakearts.webapp.security.auth.JWTLoginModule;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.JWTHeader;
import com.quakearts.webapp.security.jwt.JWTSigner;
import com.quakearts.webapp.security.jwt.JWTClaims.Claim;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.webapp.security.jwt.factory.JWTFactory;

import junit.framework.AssertionFailedError;

@RunWith(TOTPDatabaseServiceRunner.class)
public class DeviceRequestSigningServiceImplTest {

	@Inject
	private JWTGeneratorImpl jwtGenerator;	
	@Inject
	private DeviceRequestSigningServiceImpl deviceRequestSigningService;
	@Inject TOTPLoginConfiguration totpLoginConfiguration;
	
	@Test
	public void testSignRequest() throws Exception {
		AlternativeConnectionManager.run(this::expectedMessageAndResponse);
		AlternativeTOTPGenerator.simulate(true);
		AlternativeTOTPGenerator.expectedRequest("requestsensitive");
		Map<String, String> message = new HashMap<>();
		message.put("request", "sensitive");
		
		EncryptedValue seed = new EncryptedValue();
		seed.setValue("Test Value".getBytes());
		seed.setDataStoreName("test");
		
		Device device = new Device();
		device.setId("123456");
		device.setStatus(Status.ACTIVE);
		device.setSeed(seed);
		
		deviceRequestSigningService.signRequest(device, message, signedMessage->{			
			try {
				JWTClaims claims = jwtGenerator.verifyJWT(signedMessage.getBytes());
				assertThat(claims.getPrivateClaim("request"), is("sensitive"));
				assertThat(claims.getPrivateClaim("signature"), is("52ee0d38528929f5473109bf7998aeecd29ab6bddf6063888786e59d0228bb3c"));
				for(Claim claim : claims){
					if(claim.getName().equals("requestType") 
							|| claim.getName().equals("deviceId"))
						fail("Contains restricted data");
				}
			} catch (JWTException | NoSuchAlgorithmException | URISyntaxException e) {
				throw new AssertionFailedError(e.getMessage());
			}
		}, error->{
			throw new AssertionFailedError("Error callback called");
		});
	}

	@Test
	public void testSignRequestWithNoSignature() throws Exception {
		AlternativeConnectionManager.run(this::missingSignature);
		Map<String, String> message = new HashMap<>();
		message.put("request", "sensitive");
		deviceRequestSigningService.signRequest(getErrorTestDevice(), message, signedMessage->{			
			throw new AssertionFailedError("Callback should not be called");
		}, error->{
			assertThat(error, is("Error message"));
		});
	}
	
	@Test
	public void testSignRequestWithNonMatchingSignature() throws Exception {
		AlternativeConnectionManager.run(this::nonMatchingSignature);
		AlternativeTOTPGenerator.simulate(true);
		Map<String, String> message = new HashMap<>();
		message.put("request", "sensitive");
		deviceRequestSigningService.signRequest(getErrorTestDevice(), message, signedMessage->{			
			throw new AssertionFailedError("Callback should not be called");
		}, error->{
			assertThat(error, is("Invalid signature"));
		});
	}
	
	private Device getErrorTestDevice(){
		Device device = new Device();
		device.setId("123456");
		
		return device;
	}
	
	private byte[] expectedMessageAndResponse(byte[] message){
		try {
			JWTClaims jwtClaims = jwtGenerator.verifyJWT(message);
			assertThat(jwtClaims.getPrivateClaim("requestType"), is("otp-signing"));
			assertThat(jwtClaims.getPrivateClaim("deviceId"), is("123456"));
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("signature", "52ee0d38528929f5473109bf7998aeecd29ab6bddf6063888786e59d0228bb3c");
			return jwtGenerator.generateJWT(responseMap).getBytes();			
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
	
	private byte[] missingSignature(byte[] message){
		try {
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("error", "Error message");
			return jwtGenerator.generateJWT(responseMap).getBytes();			
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
	
	private byte[] nonMatchingSignature(byte[] message){
		try {
			Map<String, String> responseMap = new HashMap<>();
			responseMap.put("signature", "Non Matching");
			return jwtGenerator.generateJWT(responseMap).getBytes();			
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
	
	@Inject
	private DeviceManagementServiceImpl deviceManagementService;
	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testVerifyRequest() throws Exception {
		Optional<Device> optionalDevice = deviceManagementService
				.findDevice("testdevice1");
		
		Device device = optionalDevice.get();
		
		Map<String, Object> request = new HashMap<>();
		request.put("request", "value");
		request.put("key", "value");
		request.put("iat", 12345678901234l);
		request.put("signature", "52ee0d38528929f5473109bf7998aeecd29ab6bddf6063888786e59d0228bb3c");
		
		Map<String, ?> options = totpLoginConfiguration.getSigningConfigurationOptions();
		
		String algorithm = (String) options.get(JWTLoginModule.ALGORITHMPARAMETER);
		
		JWTSigner jwtSigner = JWTFactory.getInstance()
				.getSigner(algorithm, options);
		
		JWTHeader header = JWTFactory.getInstance().createEmptyClaimsHeader();
		JWTClaims claims = JWTFactory.getInstance().createJWTClaimsFromMap(request);
		String signature = jwtSigner.sign(header, claims);
		
		AlternativeTOTPGenerator.simulate(true);
		AlternativeTOTPGenerator.expectedRequest("keyvaluerequestvalue");
		deviceRequestSigningService.verifySignedRequest(device, signature);
	}
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testVerifyRequestWithNullSignedRequest() throws Exception {
		expectedException.expect(InvalidSignatureException.class);
		expectedException.expectMessage("signedRequest is required");
		deviceRequestSigningService.verifySignedRequest(null, null);
	}
	
	@Test
	public void testVerifyRequestWithEmptySignedRequest() throws Exception {
		expectedException.expect(InvalidSignatureException.class);
		expectedException.expectMessage("signedRequest is required");
		deviceRequestSigningService.verifySignedRequest(null, "  ");
	}

	@Test
	public void testVerifyRequestWithInvalidSignedRequest() throws Exception {
		expectedException.expect(InvalidSignatureException.class);
		expectedException.expectMessage("The provided signature could not be validated: Invalid token. Must be separated by two '.'");
		deviceRequestSigningService.verifySignedRequest(null, "invalid");
	}
	
	@Test
	public void testVerifyRequestWithIncompleteSignedRequest() throws Exception {
		expectedException.expect(InvalidSignatureException.class);
		expectedException.expectMessage("The provided signature could not be validated: Invalid token. Must be separated by two '.'");
		deviceRequestSigningService
			.verifySignedRequest(null,
					"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
				  + "eyJ0ZXN0IjoidmFsdWUiLCJ0b3RwLXRpbWVzd"
				  + "GFtcCI6IjE1MTYyMzkwMjIiLCJpYXQiOjE1MT"
				  + "YyMzkwMjJ9");
	}
	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testVerifyRequestWithForgedSignedRequest() throws Exception {
		expectedException.expect(InvalidSignatureException.class);
		expectedException.expectMessage("The provided signature could not be validated: signature does not match");
		deviceRequestSigningService
			.verifySignedRequest(deviceManagementService
					.findDevice("testdevice1").get(), 
					"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
				  + "eyJ0ZXN0IjoidmFsdWUiLCJ0ZXN0MiI6InZh"
				  + "bHVlMiIsInNpZ25hdHVyZSI6ImludmFsaWQi"
				  + "LCJpYXQiOjE1MTYyMzkwMjJ9.u8seqwoWVif"
				  + "paLKHoL5OCmIDcLRcR1zw6YfF4GZONes");
	}
	
}
