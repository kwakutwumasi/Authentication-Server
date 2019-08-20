package com.quakearts.auth.server.totp.edge.test;

import static org.junit.Assert.*;

import java.util.HashMap;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.edge.channel.impl.TOTPServerMessageHandlerImpl;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.test.alternative.AlternativeDeviceConnectionService;
import com.quakearts.auth.server.totp.edge.test.runner.MainRunner;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;
import com.quakearts.auth.server.totp.options.TOTPEdgeOptions;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.JWTHeader;
import com.quakearts.webapp.security.jwt.JWTSigner;
import com.quakearts.webapp.security.jwt.JWTVerifier;
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
		
		byte[] response = impl.handle(jwtSigner.sign(header, claims).getBytes());		
		JWTVerifier verifier = factory.getVerifier(totpEdgeOptions.getJwtalgorithm(),
						totpEdgeOptions.getJwtOptions());		
		verifier.verify(response);
		JWTClaims responseClaims = verifier.getClaims();
		
		assertThat(responseClaims.getPrivateClaim("test"), is("response"));
	}
	
	@Test
	public void testHandleEcho() throws Exception {
		byte[] echo = new byte[1];
		byte[] response = impl.handle(echo);
		assertThat(response, is(echo));
	}

	@Test
	public void testHandleJWTException() throws Exception {
		AlternativeDeviceConnectionService.returnPayload(payload->{
			throw new UnconnectedDeviceException();
		});
		
		JWTFactory factory = JWTFactory.getInstance();
		JWTSigner jwtSigner = factory.getSigner(totpEdgeOptions.getJwtalgorithm(), totpEdgeOptions.getJwtOptions());
		
		JWTHeader header = factory.createEmptyClaimsHeader();
		JWTClaims claims = factory.createEmptyClaims();
		
		claims.addPrivateClaim("test", "message");
		
		byte[] response = impl.handle(jwtSigner.sign(header, claims).getBytes());		
		
		assertThat(response, is(notNullValue()));
		assertThat(response.length, is(1));
		assertThat(response[0], is((byte)255));
	}
}
