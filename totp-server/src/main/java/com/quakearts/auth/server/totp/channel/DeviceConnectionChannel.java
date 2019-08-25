package com.quakearts.auth.server.totp.channel;

import java.util.Map;

import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.function.CheckedConsumer;

public interface DeviceConnectionChannel {
	void sendMessage(Map<String, String> jsonMap, CheckedConsumer<Map<String, String>, TOTPException> callback) 
			throws TOTPException;
}
