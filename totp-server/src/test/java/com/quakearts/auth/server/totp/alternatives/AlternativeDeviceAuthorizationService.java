package com.quakearts.auth.server.totp.alternatives;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.function.CheckedConsumer;
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
	
	@Override
	public void requestOTPCode(String deviceId, CheckedConsumer<String, TOTPException> callback) throws TOTPException {
		if(throwException != null) {
			TOTPException toreturn = throwException;
			throwException = null;
			throw toreturn;
		}
		
		if(doNothing) {
			doNothing = false;
			return;
		}
		
		wrapped.requestOTPCode(deviceId, callback);
	}

}
