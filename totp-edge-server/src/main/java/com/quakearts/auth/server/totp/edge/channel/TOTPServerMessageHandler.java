package com.quakearts.auth.server.totp.edge.channel;

import com.quakearts.webapp.security.jwt.exception.JWTException;

public interface TOTPServerMessageHandler {
	byte[] handle(byte[] message) throws JWTException;
}
