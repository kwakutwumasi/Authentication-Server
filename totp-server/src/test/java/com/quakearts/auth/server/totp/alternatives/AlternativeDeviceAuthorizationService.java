package com.quakearts.auth.server.totp.alternatives;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.exception.MessageGenerationException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.rest.authorization.DeviceAuthorizationService;
import com.quakearts.auth.server.totp.rest.authorization.impl.DeviceAuthorizationServiceImpl;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@Singleton
public class AlternativeDeviceAuthorizationService implements DeviceAuthorizationService {

	@Inject
	private DeviceAuthorizationServiceImpl wrapped;
	
	private static TOTPException throwException;
	
	public static void throwException(TOTPException newThrowException) {
		throwException = newThrowException;
	}
	
	@Override
	public String requestOTPCode(String deviceId) throws UnconnectedDeviceException, MessageGenerationException {
		if(throwException != null) {
			TOTPException toreturn = throwException;
			throwException = null;
			if(toreturn instanceof MessageGenerationException) {
				throw (MessageGenerationException) toreturn;
			} else if (toreturn instanceof UnconnectedDeviceException){
				throw (UnconnectedDeviceException) toreturn;
			}
		}
		
		return wrapped.requestOTPCode(deviceId);
	}

}
