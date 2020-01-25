package com.quakearts.auth.server.totp.device.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.auth.server.totp.device.DeviceConnectionExecutorService;
import com.quakearts.auth.server.totp.options.TOTPOptions;

@Singleton
public class DeviceConnectionExecutorServiceImpl implements DeviceConnectionExecutorService {

	@Inject
	private TOTPOptions totpOptions;

	private ExecutorService executorService;
	
	@Override
	public ExecutorService getExecutorService() {
		if(executorService==null) {
			executorService = Executors.newFixedThreadPool(totpOptions.getExecutorServiceThreads());
		}
		return executorService;
	}

}
