package com.quakearts.auth.server.totp.signing.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.quakearts.auth.server.totp.channel.DeviceConnectionChannel;
import com.quakearts.auth.server.totp.exception.InvalidSignatureException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.generator.TOTPGenerator;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.signing.DeviceRequestSigningService;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.JWTHeader;
import com.quakearts.webapp.security.jwt.JWTSigner;
import com.quakearts.webapp.security.jwt.JWTVerifier;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.webapp.security.jwt.factory.JWTFactory;

public class DeviceRequestSigningServiceImpl implements DeviceRequestSigningService {

	private static final String TIMESTAMP = "totp-timestamp";

	private static final String HS256 = "HS256";

	@Inject
	private DeviceConnectionChannel deviceConnectionChannel;
	
	@Inject
	private TOTPGenerator totpGenerator;

	@Override
	public void signRequest(String deviceId, Map<String, String> requestMap, Consumer<String> callback,
			Consumer<String> errorCallback) throws TOTPException {
		requestMap.put("requestType", "otp-signing");
		requestMap.put("deviceId", deviceId);
		deviceConnectionChannel.sendMessage(requestMap, response->{
			String otp = response.get("otp");
			String timestamp = response.get(TIMESTAMP);
			if(otp != null && timestamp != null) {
				requestMap.put(TIMESTAMP, timestamp);
				JWTFactory factory = JWTFactory.getInstance();
				JWTSigner jwtSigner;
				try {
					jwtSigner = factory.getSigner(HS256, createOptionsWith(otp));
					JWTHeader header = factory.createEmptyClaimsHeader();
					JWTClaims claims = factory.createJWTClaimsFromMap(requestMap);
					callback.accept(jwtSigner.sign(header, claims));
				} catch (JWTException e) {
					errorCallback.accept(e.getMessage());
				}
			} else {
				errorCallback.accept(response.get("error"));
			}
		});
	}

	private Map<String, Object> createOptionsWith(String otp) {
		Map<String, Object> options = new HashMap<>();
		options.put("secret", otp);
		return options;
	}

	@Override
	public void verifySignedRequest(Device device, String signedRequest) 
			throws InvalidSignatureException {
		if(signedRequest == null || signedRequest.trim().isEmpty())
			throw new InvalidSignatureException("signedRequest is required");
		
		int firstPointIndex = signedRequest.indexOf('.');
		int secondPointIndex = signedRequest.lastIndexOf('.');
		
		if(firstPointIndex == -1 || firstPointIndex == secondPointIndex)
			throw new InvalidSignatureException("signedRequest must be a valid JWT token with a header, body and signed trailer separated by '.'");
		
		String claimsString = signedRequest.substring(firstPointIndex+1, secondPointIndex);
		
		JWTFactory factory = JWTFactory.getInstance();
		JWTClaims claims = factory.createJWTClaimsFromBytes(claimsString.getBytes());
		long timestamp = Long.parseLong(claims.getPrivateClaim(TIMESTAMP));
		
		String[] totp = totpGenerator.generateFor(device, timestamp);
		
		try {
			JWTVerifier verifier = factory.getVerifier(HS256, createOptionsWith(totp[0]));
			verifier.verify(signedRequest);
		} catch (JWTException e) {
			throw new InvalidSignatureException(e);
		}
	}

}