package com.quakearts.auth.server.totp.rest.authorization;

import com.quakearts.auth.server.totp.exception.MessageGenerationException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;

public interface DeviceAuthorizationService {
	String requestOTPCode(String deviceId) 
		throws UnconnectedDeviceException, MessageGenerationException;
}
