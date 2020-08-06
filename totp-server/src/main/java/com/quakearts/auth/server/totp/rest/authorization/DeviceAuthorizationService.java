package com.quakearts.auth.server.totp.rest.authorization;

import java.util.Map;
import java.util.function.Consumer;
import com.quakearts.auth.server.totp.exception.TOTPException;

public interface DeviceAuthorizationService {
	void requestOTPCode(String deviceId, Map<String, String> authenticationData, Consumer<String> callback, Consumer<String> errorCallback) 
			throws TOTPException;
}
