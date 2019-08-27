package com.quakearts.auth.server.totp.alternatives;

import java.util.function.Consumer;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.rest.authorization.DeviceAuthorizationService;
import com.quakearts.auth.server.totp.rest.authorization.impl.DeviceAuthorizationServiceImpl;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@Singleton
public class AlternativeDeviceAuthorizationService implements DeviceAuthorizationService {

	@Inject
	private DeviceAuthorizationServiceImpl wrapped;

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
	public void requestOTPCode(String deviceId, Consumer<String> callback, Consumer<String> errorCallback) throws TOTPException {
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
		
		wrapped.requestOTPCode(deviceId, callback, errorCallback);
	}

}
