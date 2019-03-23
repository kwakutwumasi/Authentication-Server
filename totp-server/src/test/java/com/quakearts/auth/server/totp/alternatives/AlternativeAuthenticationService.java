package com.quakearts.auth.server.totp.alternatives;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.authentication.AuthenticationService;
import com.quakearts.auth.server.totp.authentication.impl.AuthenticationServiceImpl;
import com.quakearts.auth.server.totp.model.Device;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@Singleton
public class AlternativeAuthenticationService implements AuthenticationService {

	private static TestAuthenticateFunction returnAuthenticate;
	
	@FunctionalInterface
	public static interface TestAuthenticateFunction {
		boolean authenticate(Device device, String totpToken);
	}
	
	public static void returnAuthenticate(TestAuthenticateFunction returnAuthenticate) {
		AlternativeAuthenticationService.returnAuthenticate = returnAuthenticate;
	}
	
	private static TestReturnLockedFunction returnLocked;
	
	@FunctionalInterface
	public static interface TestReturnLockedFunction {
		boolean isLocked(Device device);
	}
	
	public static void returnLocked(TestReturnLockedFunction returnLocked) {
		AlternativeAuthenticationService.returnLocked = returnLocked;
	}

	@Inject
	private AuthenticationServiceImpl authenticationService;
	
	@Override
	public boolean authenticate(Device device, String totpToken) {
		if(returnAuthenticate!=null){
			TestAuthenticateFunction toreturn = returnAuthenticate;
			returnAuthenticate = null;
			return toreturn.authenticate(device, totpToken);
		}
		
		return authenticationService.authenticate(device, totpToken);
	}

	@Override
	public boolean isLocked(Device device) {
		if(returnLocked!=null){
			TestReturnLockedFunction toreturn = returnLocked;
			returnAuthenticate = null;
			return toreturn.isLocked(device);
		}
		
		return authenticationService.isLocked(device);
	}

}
