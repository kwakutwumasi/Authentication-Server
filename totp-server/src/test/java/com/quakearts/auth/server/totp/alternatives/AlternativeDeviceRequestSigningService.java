package com.quakearts.auth.server.totp.alternatives;

import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.exception.InvalidSignatureException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.signing.DeviceRequestSigningService;
import com.quakearts.auth.server.totp.signing.impl.DeviceRequestSigningServiceImpl;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@Singleton
public class AlternativeDeviceRequestSigningService implements DeviceRequestSigningService {

	@Inject
	private DeviceRequestSigningServiceImpl wrapped;
	
	private static boolean doNothing;
	public static void doNothing(boolean newDoNothing) {
		doNothing = newDoNothing;
	}
	
	private static TOTPException throwException;
	public static void throwException(TOTPException newThrowException) {
		throwException = newThrowException;
	}

	private static boolean callErrorCallback;
	public static void callErrorCallback(boolean newCallErrorCallback) {
		callErrorCallback = newCallErrorCallback;
	}

	@Override
	public void signRequest(String deviceId, Map<String, String> requestMap, Consumer<String> callback,
			Consumer<String> errorCallback) throws TOTPException {
		if(throwException != null) {
			TOTPException toreturn = throwException;
			throwException = null;
			throw toreturn;
		}
		
		if(doNothing) {
			doNothing = false;
			return;
		}
		
		if(callErrorCallback) {
			callErrorCallback = false;
			errorCallback.accept("Error message");
			return;
		}
		
		wrapped.signRequest(deviceId, requestMap, callback, errorCallback);
	}
	
	@Override
	public void verifySignedRequest(Device device, String signedRequest) throws InvalidSignatureException {
		wrapped.verifySignedRequest(device, signedRequest);
	}

}
