package com.quakearts.auth.server.totp.authentication.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.authentication.AuthenticationService;
import com.quakearts.auth.server.totp.generator.TOTPGenerator;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.webapp.security.auth.util.AttemptChecker;

@Singleton
public class AuthenticationServiceImpl implements AuthenticationService {

	private static final String TOTP_SERVER = "TOTP-SERVER";
	
	private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
	
	@Inject
	private TOTPOptions options;
	private AttemptChecker attemptChecker;
	@Inject
	private TOTPGenerator generator;
		
	@Override
	public boolean authenticate(Device device, String totpToken) {
		log.debug("Authenticating device with itemCount {}", device.getItemCount());
		if(!isLocked(device)){
			getAttemptChecker().incrementAttempts(device.getId());
			String[] compareTokens = generator.generateFor(device, System.currentTimeMillis());
			log.debug("Multiple tokens for device with itemCount: {} - {}", device.getItemCount(), 
					compareTokens[1]!=null);
			if(compareTokens[0].equals(totpToken)
					|| (compareTokens[1]!=null && compareTokens[1].equals(totpToken))){
				log.debug("Device with itemCount: {} authenticated", device.getItemCount());
				getAttemptChecker().reset(device.getId());
				return true;
			}
			log.debug("Device with itemCount {} failed authentication", device.getItemCount());
		} else {
			log.debug("Device with itemCount {} is locked", device.getItemCount());
		}
		return false;
	}
	
	private AttemptChecker getAttemptChecker(){
		if(attemptChecker == null){
			log.debug("Creating attempt checker");
			AttemptChecker.createChecker(TOTP_SERVER, options.getMaxAttempts(), options.getLockoutTime());
			attemptChecker = AttemptChecker.getChecker(TOTP_SERVER);
		}
		
		log.debug("Returning attempt checker");
		return attemptChecker;
	}

	@Override
	public boolean isLocked(Device device) {
		return getAttemptChecker().isLocked(device.getId());
	}

}
