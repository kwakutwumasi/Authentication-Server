package com.quakearts.auth.server.totp.edge.test;

import static org.junit.Assert.*;

import java.util.HashMap;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.edge.channel.Message;
import com.quakearts.auth.server.totp.edge.channel.impl.TOTPServerMessageHandlerImpl;
import com.quakearts.auth.server.totp.edge.test.alternative.AlternativeDeviceConnectionService;
import com.quakearts.auth.server.totp.edge.test.alternative.AlternativeTOTPEdgeOptions;
import com.quakearts.auth.server.totp.edge.test.runner.MainRunner;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;
import com.quakearts.auth.server.totp.options.TOTPEdgeOptions;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.JWTHeader;
import com.quakearts.webapp.security.jwt.JWTSigner;
import com.quakearts.webapp.security.jwt.JWTVerifier;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.webapp.security.jwt.factory.JWTFactory;

@RunWith(MainRunner.class)
public class TOTPServerMessageHandlerImplTest {

	@Inject
	private TOTPServerMessageHandlerImpl impl;
	
	@Inject
	private TOTPEdgeOptions totpEdgeOptions;
	
	@Test
	public void testHandle() throws Exception {
		AlternativeDeviceConnectionService.returnPayload(payload->{
			assertThat(payload, is(notNullValue()));
			assertThat(payload.getMessage(), is(notNullValue()));
			assertThat(payload.getMessage().get("test"), is("message"));
			Payload responsePayload = new Payload();
			responsePayload.setMessage(new HashMap<>());
			responsePayload.getMessage().put("test", "response");
			return responsePayload;
		});
		
		JWTFactory factory = JWTFactory.getInstance();
		JWTSigner jwtSigner = factory.getSigner(totpEdgeOptions.getJwtalgorithm(), totpEdgeOptions.getJwtOptions());
		
		JWTHeader header = factory.createEmptyClaimsHeader();
		JWTClaims claims = factory.createEmptyClaims();
		
		claims.addPrivateClaim("test", "message");
		
		impl.handle(new Message(1l, jwtSigner.sign(header, claims).getBytes()), responseMessage->{			
			JWTVerifier verifier;
			try {
				verifier = factory.getVerifier(totpEdgeOptions.getJwtalgorithm(),
						totpEdgeOptions.getJwtOptions());
				verifier.verify(responseMessage.getValue());
			} catch (JWTException e) {
				throw new AssertionError(e);
			}
			JWTClaims responseClaims = verifier.getClaims();
			
			assertThat(responseClaims.getPrivateClaim("test"), is("response"));
		});
	}
	
	@Test
	public void testHandleEcho() throws Exception {
		byte[] echo = new byte[1];
		impl.handle(new Message(1l, echo), responseMessage->{			
			assertThat(responseMessage.getValue(), is(echo));
		});
	}

	@Test
	public void testHandleJWTException() throws Exception {
		AlternativeDeviceConnectionService.returnPayload(payload->{
			AlternativeTOTPEdgeOptions.returnInvalidAlgorithm(true);
			Payload response = new Payload();
			response.setMessage(new HashMap<>());
			response.getMessage().put("test", "response");
			return response;
		});
				
		JWTFactory factory = JWTFactory.getInstance();
		JWTSigner jwtSigner = factory.getSigner(totpEdgeOptions.getJwtalgorithm(), totpEdgeOptions.getJwtOptions());
		
		JWTHeader header = factory.createEmptyClaimsHeader();
		JWTClaims claims = factory.createEmptyClaims();
		
		claims.addPrivateClaim("test", "message");
		
		impl.handle(new Message(1l, jwtSigner.sign(header, claims).getBytes()), response->{			
			assertThat(response, is(notNullValue()));
			assertThat(response.getValue(), is(notNullValue()));
			assertThat(response.getValue().length, is(1));
			assertThat(response.getValue()[0], is((byte)255));
		});
	}
}
