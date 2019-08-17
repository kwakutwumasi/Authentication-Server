package com.quakearts.auth.server.totp.channel.impl;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.auth.server.totp.channel.ConnectionManager;
import com.quakearts.auth.server.totp.channel.DeviceConnectionChannel;
import com.quakearts.auth.server.totp.exception.MessageGenerationException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.generator.JWTGenerator;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.JWTClaims.Claim;
import com.quakearts.webapp.security.jwt.exception.JWTException;

@Singleton
public class DeviceConnectionChannelImpl implements DeviceConnectionChannel {

	@Inject
	private ConnectionManager connectionManager;
	@Inject
	private JWTGenerator jwtGenerator;
	
	@Override
	public Map<String, String> sendMessage(Map<String, String> requestMap) 
			throws UnconnectedDeviceException, MessageGenerationException {
		byte[] bites;
		try {
			bites = jwtGenerator.generateJWT(requestMap).getBytes();
		} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
			throw new MessageGenerationException(e);
		}
		
		byte[] response = connectionManager.send(bites);
		try {
			JWTClaims jwtClaims = jwtGenerator.verifyJWT(response);
			Map<String, String> responseMap = new HashMap<>();
			for(Claim claim:jwtClaims) {
				responseMap.put(claim.getName(), claim.getValue());
			}
			
			return responseMap;
		} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
			throw new MessageGenerationException(e);
		}	
	}

}
