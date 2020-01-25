package com.quakearts.auth.server.totp.signing;

import java.util.Map;
import java.util.function.Consumer;

import com.quakearts.auth.server.totp.exception.InvalidSignatureException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.model.Device;

public interface DeviceRequestSigningService {
	void signRequest(Device device, Map<String, String> requestMap, Consumer<String> callback, 
			Consumer<String> errorCallback) throws TOTPException;
	void verifySignedRequest(Device device, String signedRequest)
		throws InvalidSignatureException;
}
