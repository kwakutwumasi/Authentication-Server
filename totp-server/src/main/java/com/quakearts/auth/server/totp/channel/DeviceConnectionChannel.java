package com.quakearts.auth.server.totp.channel;

import java.util.Map;

import com.quakearts.auth.server.totp.exception.MessageGenerationException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;

public interface DeviceConnectionChannel {
	Map<String, String> sendMessage(Map<String, String> jsonMap) 
			throws UnconnectedDeviceException, MessageGenerationException;
}
