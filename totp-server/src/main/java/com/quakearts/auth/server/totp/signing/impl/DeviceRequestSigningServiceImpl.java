package com.quakearts.auth.server.totp.signing.impl;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.channel.DeviceConnectionChannel;
import com.quakearts.auth.server.totp.exception.InvalidSignatureException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.generator.JWTGenerator;
import com.quakearts.auth.server.totp.generator.TOTPGenerator;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.signing.DeviceRequestSigningService;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.exception.JWTException;

public class DeviceRequestSigningServiceImpl implements DeviceRequestSigningService {

	private static final Logger log = LoggerFactory.getLogger(DeviceRequestSigningService.class);

	private static final String SIGNATURE = "signature";
	private static final String NOTINCLUDED = SIGNATURE+",iat";
	
	@Inject
	private DeviceConnectionChannel deviceConnectionChannel;
	
	@Inject
	private TOTPGenerator totpGenerator;
	
	@Inject
	private JWTGenerator generator;

	@Override
	public void signRequest(Device device, Map<String, String> requestMap, Consumer<String> callback,
			Consumer<String> errorCallback) throws TOTPException {
		requestMap.put("requestType", "otp-signing");
		requestMap.put("deviceId", device.getId());
		
		log.debug("Sending signing request message with hashCode: {} for device with itemCount: {}", 
				requestMap.hashCode(), device.getItemCount());

		deviceConnectionChannel.sendMessage(requestMap, response->{
			String signature = response.get(SIGNATURE);
			if(signature != null 
					&& signature.equalsIgnoreCase(generateCompareSignature(device, requestMap))) {
				requestMap.put(SIGNATURE, signature);
				try {
					callback.accept(generator.generateJWT(requestMap));
				} catch (JWTException | NoSuchAlgorithmException | URISyntaxException e) {
					errorCallback.accept(e.getMessage());
				}
			} else {
				errorCallback.accept(response.containsKey("error")? response.get("error"):"Invalid signature");
			}
		});
	}

	private String generateCompareSignature(Device device, Map<String, ?> requestMap) {
		requestMap.remove("requestType");
		requestMap.remove("deviceId");
		TreeMap<String, Object> sortedMap = new TreeMap<>();
		requestMap.entrySet().forEach(entry->sortedMap.put(entry.getKey(), entry.getValue()));
		return sign(device, sortedMap);
	}

	@Override
	public void verifySignedRequest(Device device, String signedRequest) 
			throws InvalidSignatureException {
		if(signedRequest == null || signedRequest.trim().isEmpty())
			throw new InvalidSignatureException("signedRequest is required");
		
		JWTClaims claims;
		try {
			claims = generator.verifyRequestJWT(signedRequest.getBytes());
		} catch (JWTException | NoSuchAlgorithmException | URISyntaxException e) {
			throw new InvalidSignatureException(e);
		}
		
		TreeMap<String, Object> sortedMap = new TreeMap<>();
		claims.iterator().forEachRemaining(claim->sortedMap.put(claim.getName(), claim.getValue()));
		String signature = sign(device, sortedMap);
		if(!signature.equalsIgnoreCase(claims.getPrivateClaim(SIGNATURE))) {
			throw new InvalidSignatureException("signature does not match");
		}
	}

	private String sign(Device device, SortedMap<String, Object> sortedMap) {
		String signatureBase = sortedMap.entrySet().stream().map(entry->NOTINCLUDED
				.contains(entry.getKey())?"":entry.getKey()+entry.getValue())
			.collect(Collectors.joining());
		return totpGenerator.signRequest(device, signatureBase);
	}

}
