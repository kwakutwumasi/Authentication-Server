package com.quakearts.auth.server.totp.edge.channel.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.edge.channel.Callback;
import com.quakearts.auth.server.totp.edge.channel.DeviceConnectionService;
import com.quakearts.auth.server.totp.edge.channel.Message;
import com.quakearts.auth.server.totp.edge.channel.TOTPServerMessageHandler;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;
import com.quakearts.auth.server.totp.options.TOTPEdgeOptions;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.JWTVerifier;
import com.quakearts.webapp.security.jwt.RegisteredNames;
import com.quakearts.webapp.security.jwt.JWTClaims.Claim;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.webapp.security.jwt.factory.JWTFactory;

@Singleton
public class TOTPServerMessageHandlerImpl implements TOTPServerMessageHandler {

	private static final Logger log = LoggerFactory.getLogger(TOTPServerMessageHandler.class);
	private static final byte[] NORESPONSE = new byte[] {(byte) 255};

	@Inject
	private DeviceConnectionService deviceConnectionService;
	
	@Inject
	private TOTPEdgeOptions totpEdgeOptions;
	
	@Override
	public void handle(Message message, Callback<Message, IOException> callback) 
			throws JWTException, IOException, UnconnectedDeviceException {
		if(message.getValue().length != 1) {
			JWTFactory factory = JWTFactory.getInstance();
			
			JWTVerifier verifier = factory.getVerifier(totpEdgeOptions.getJwtalgorithm(),
							totpEdgeOptions.getJwtOptions());
			
			verifier.verify(message.getValue());
			JWTClaims jwtClaims = verifier.getClaims();
						
			Map<String, String> request = new HashMap<>();
			for(Claim claim:jwtClaims) {
				request.put(claim.getName(), claim.getValue());
			}
			
			if(request.containsKey("ping")){
				JWTClaims responseClaims = factory.createEmptyClaims();
				responseClaims.addPrivateClaim("connected", ""+deviceConnectionService
						.isConnected(request.get("deviceId")));
				byte[] responseMessage = generateMessageBytes(factory, responseClaims);
				callback.execute(new Message(message.getTicket(), responseMessage));
				return;
			}
			
			Payload payload = new Payload();
			payload.setMessage(request);
			deviceConnectionService.send(payload, responsePayload->{
					
				JWTClaims responseClaims = factory.createEmptyClaims();
				
				responsePayload.getMessage().entrySet().stream().filter(entry->!RegisteredNames.IAT.equals(entry.getKey()))
					.forEach(entry->responseClaims.addPrivateClaim(entry.getKey(), entry.getValue()));
				
				byte[] responseMessage = generateMessageBytes(factory, responseClaims);
				try {
					callback.execute(new Message(message.getTicket(), responseMessage));
				} catch (IOException e) {
					log.error("Error processing processing response for message {}", message.getTicket());
				}
			});
		} else {
			callback.execute(message);
		}
	}

	protected byte[] generateMessageBytes(JWTFactory factory, JWTClaims responseClaims) {
		byte[] responseMessage; 
		try {
			responseMessage = factory.getSigner(totpEdgeOptions.getJwtalgorithm(), totpEdgeOptions.getJwtOptions())
					.sign(factory.createEmptyClaimsHeader(), responseClaims).getBytes();
		} catch (JWTException e) {
			log.error("Error generating response message.", e);
			responseMessage = NORESPONSE;
		}
		return responseMessage;
	}

}
