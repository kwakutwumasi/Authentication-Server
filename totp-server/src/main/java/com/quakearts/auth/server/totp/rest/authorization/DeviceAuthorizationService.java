package com.quakearts.auth.server.totp.rest.authorization;


import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.function.CheckedConsumer;

public interface DeviceAuthorizationService {
	void requestOTPCode(String deviceId, CheckedConsumer<String, TOTPException> callback) 
			throws TOTPException;
}
