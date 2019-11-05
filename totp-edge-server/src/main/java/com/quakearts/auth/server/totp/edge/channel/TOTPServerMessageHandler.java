package com.quakearts.auth.server.totp.edge.channel;

import java.io.IOException;

import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.webapp.security.jwt.exception.JWTException;

public interface TOTPServerMessageHandler {
	void handle(Message message, Callback<Message, IOException> callback) 
			throws JWTException, IOException, UnconnectedDeviceException;
}
