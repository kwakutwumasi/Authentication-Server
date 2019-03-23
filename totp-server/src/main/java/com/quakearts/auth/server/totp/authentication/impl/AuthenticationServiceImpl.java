package com.quakearts.auth.server.totp.authentication.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.auth.server.totp.authentication.AuthenticationService;
import com.quakearts.auth.server.totp.generator.TOTPGenerator;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.webapp.security.auth.util.AttemptChecker;

@Singleton
public class AuthenticationServiceImpl implements AuthenticationService {

	private static final String TOTP_SERVER = "TOTP-SERVER";
	@Inject
	private TOTPOptions options;
	private AttemptChecker attemptChecker;
	@Inject
	private TOTPGenerator generator;
		
	@Override
	public boolean authenticate(Device device, String totpToken) {
		if(!isLocked(device)){
			getAttemptChecker().incrementAttempts(device.getId());
			String[] compareTokens = generator.generateFor(device, System.currentTimeMillis());
			if(compareTokens[0].equals(totpToken)
					|| (compareTokens[1]!=null && compareTokens[1].equals(totpToken))){
				getAttemptChecker().reset(device.getId());
				return true;
			}
		}
		return false;
	}
	
	private AttemptChecker getAttemptChecker(){
		if(attemptChecker == null){
			AttemptChecker.createChecker(TOTP_SERVER, options.getMaxAttempts(), options.getLockoutTime());
			attemptChecker = AttemptChecker.getChecker(TOTP_SERVER);
		}
		
		return attemptChecker;
	}

	@Override
	public boolean isLocked(Device device) {
		return getAttemptChecker().isLocked(device.getId());
	}

}
