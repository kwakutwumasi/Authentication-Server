package com.quakearts.auth.server.totp.edge.test.alternative;

import java.io.IOException;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.edge.channel.Callback;
import com.quakearts.auth.server.totp.edge.channel.Message;
import com.quakearts.auth.server.totp.edge.channel.TOTPServerMessageHandler;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
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
	public void handle(Message message, Callback<Message, IOException> callback)
			throws JWTException, IOException, UnconnectedDeviceException {
		callback.execute(new Message(message.getTicket(), 
				returnBytes.apply(message.getValue())));
	}

	@FunctionalInterface
	public static interface Function {
		byte[] apply(byte[] bite) throws JWTException;
	}
}
