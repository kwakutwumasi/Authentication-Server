package com.quakearts.auth.server.totp.rest.authorization.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.auth.server.totp.rest.authorization.AuthorizationExecutorService;

@Singleton
public class AuthorizationExecutorServiceImpl implements AuthorizationExecutorService {

	@Inject
	private TOTPOptions totpOptions;

	private ExecutorService executorService;
	
	@Override
	public ExecutorService getExecutorService() {
		if(executorService==null) {
			executorService = Executors.newFixedThreadPool(totpOptions.getAuthorizationThreads());
		}
		return executorService;
	}

}
