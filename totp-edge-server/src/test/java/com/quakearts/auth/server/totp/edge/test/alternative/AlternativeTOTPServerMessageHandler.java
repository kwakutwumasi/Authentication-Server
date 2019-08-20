package com.quakearts.auth.server.totp.edge.test.alternative;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.edge.channel.TOTPServerMessageHandler;
import com.quakearts.webapp.security.jwt.exception.JWTException;

@Alternative
@Singleton
@Priority(Interceptor.Priority.APPLICATION)
public class AlternativeTOTPServerMessageHandler 
	implements TOTPServerMessageHandler {

	private static Function returnBytes = bites->bites;
	
	public static void returnBytes(Function newReturnBytes) {
		returnBytes = newReturnBytes;
	}
	
	@Override
	public byte[] handle(byte[] message) throws JWTException {
		return returnBytes.apply(message);
	}

	@FunctionalInterface
	public static interface Function {
		byte[] apply(byte[] bite) throws JWTException;
	}
}
