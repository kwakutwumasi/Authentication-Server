package com.quakearts.auth.server.totp.rest.authorization;

import java.util.Map;
import java.util.function.Consumer;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.model.Device;

public interface DeviceAuthorizationService {
	void requestOTPCode(Device device, Map<String, String> authenticationData, Consumer<String> callback, Consumer<String> errorCallback) 
			throws TOTPException;
}
