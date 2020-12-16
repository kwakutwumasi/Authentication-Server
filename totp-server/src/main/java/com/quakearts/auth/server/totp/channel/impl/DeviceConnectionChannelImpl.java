package com.quakearts.auth.server.totp.channel.impl;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.channel.ConnectionManager;
import com.quakearts.auth.server.totp.channel.DeviceConnectionChannel;
import com.quakearts.auth.server.totp.exception.CallbackException;
import com.quakearts.auth.server.totp.exception.MessageGenerationException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.generator.JWTGenerator;
import com.quakearts.auth.server.totp.utils.MaskUtil;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.JWTClaims.Claim;
import com.quakearts.webapp.security.jwt.exception.JWTException;

@Singleton
public class DeviceConnectionChannelImpl implements DeviceConnectionChannel {
	
	private static final Logger log = LoggerFactory.getLogger(DeviceConnectionChannel.class);
	
	@Inject
	private ConnectionManager connectionManager;
	@Inject
	private JWTGenerator jwtGenerator;
	
	@Override
	public void sendMessage(Map<String, String> requestMap, Consumer<Map<String, String>> callback) 
			throws TOTPException {
		if(log.isDebugEnabled())
			log.debug("Sending request: {}", MaskUtil.mask(requestMap));
		byte[] bites;
		try {
			bites = jwtGenerator.generateJWT(requestMap).getBytes();
		} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
			throw new MessageGenerationException(e);
		}
		
		if(log.isDebugEnabled())
			log.debug("Processed message for request: {}. Sending....", MaskUtil.mask(requestMap));
		
		connectionManager.send(bites, response->{
			if(log.isDebugEnabled())
				log.debug("Processing response for request: {}", MaskUtil.mask(requestMap));

			Map<String, String> responseMap = new HashMap<>();
			if(response.length > 1) {		
				try {
					JWTClaims jwtClaims = jwtGenerator.verifyJWT(response);
					for(Claim claim:jwtClaims) {
						responseMap.put(claim.getName(), claim.getValue());
					}					
				} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
					throw new CallbackException(e);
				}				
			} else {
				responseMap.put("error", "A connection has not been registered, or it may have been terminated by an error");
			}
			callback.accept(responseMap);
			if(log.isDebugEnabled())
				log.debug("Processed response for request: {}", MaskUtil.mask(requestMap));
		});
		if(log.isDebugEnabled())
			log.debug("Sent message for request: {}", MaskUtil.mask(requestMap));
	}

}
