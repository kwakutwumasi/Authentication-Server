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
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.function.CheckedConsumer;
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
	public void sendMessage(Map<String, String> requestMap, CheckedConsumer<Map<String, String>, TOTPException> callback) 
			throws TOTPException {
		byte[] bites;
		try {
			bites = jwtGenerator.generateJWT(requestMap).getBytes();
		} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
			throw new MessageGenerationException(e);
		}
		
		connectionManager.send(bites, response->{
			try {
				JWTClaims jwtClaims = jwtGenerator.verifyJWT(response);
				Map<String, String> responseMap = new HashMap<>();
				for(Claim claim:jwtClaims) {
					responseMap.put(claim.getName(), claim.getValue());
				}
				
				callback.accept(responseMap);
			} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
				throw new MessageGenerationException(e);
			}			
		});
	}

}
