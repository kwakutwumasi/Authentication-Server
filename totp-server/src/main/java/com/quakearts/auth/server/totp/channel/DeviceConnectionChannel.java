package com.quakearts.auth.server.totp.channel;

import java.util.Map;
import java.util.function.Consumer;

import com.quakearts.auth.server.totp.exception.TOTPException;

public interface DeviceConnectionChannel {
	void sendMessage(Map<String, String> jsonMap, Consumer<Map<String, String>> callback) 
			throws TOTPException;
}
