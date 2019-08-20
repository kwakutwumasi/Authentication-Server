package com.quakearts.auth.server.totp.edge.channel.impl;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnectionService;
import com.quakearts.auth.server.totp.edge.channel.TOTPServerMessageHandler;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;
import com.quakearts.auth.server.totp.options.TOTPEdgeOptions;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.JWTVerifier;
import com.quakearts.webapp.security.jwt.JWTClaims.Claim;
import com.quakearts.webapp.security.jwt.JWTHeader;
import com.quakearts.webapp.security.jwt.JWTSigner;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.webapp.security.jwt.factory.JWTFactory;

@Singleton
public class TOTPServerMessageHandlerImpl implements TOTPServerMessageHandler {

	private static final byte[] NORESPONSE = new byte[] {(byte) 255};

	@Inject
	private DeviceConnectionService deviceConnectionService;
	
	@Inject
	private TOTPEdgeOptions totpEdgeOptions;
	
	@Override
	public byte[] handle(byte[] message) throws JWTException {
		if(message.length == 1) {
			return message;
		}
		
		JWTFactory factory = JWTFactory.getInstance();
		
		JWTVerifier verifier = factory.getVerifier(totpEdgeOptions.getJwtalgorithm(),
						totpEdgeOptions.getJwtOptions());
		
		verifier.verify(message);
		JWTClaims jwtClaims = verifier.getClaims();
		Map<String, String> request = new HashMap<>();
		for(Claim claim:jwtClaims) {
			request.put(claim.getName(), claim.getValue());
		}
		
		Payload payload = new Payload();
		payload.setMessage(request);
		Payload response;
		try {
			response = deviceConnectionService.send(payload);
		} catch (UnconnectedDeviceException e) {
			return NORESPONSE;
		}
		
		JWTHeader header = factory.createEmptyClaimsHeader();
		JWTClaims responseClaims = factory.createEmptyClaims();
		
		response.getMessage().forEach(responseClaims::addPrivateClaim);
		
		JWTSigner jwtSigner = factory.getSigner(totpEdgeOptions.getJwtalgorithm(), totpEdgeOptions.getJwtOptions());
		return jwtSigner.sign(header, responseClaims).getBytes();
	}

}
