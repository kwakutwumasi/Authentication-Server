package com.quakearts.auth.server.totp.device.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.device.DeviceConnectionExecutorService;
import com.quakearts.auth.server.totp.options.TOTPOptions;

@Singleton
public class DeviceConnectionExecutorServiceImpl implements DeviceConnectionExecutorService {

	private static final Logger log = LoggerFactory.getLogger(DeviceConnectionExecutorService.class);
	
	@Inject
	private TOTPOptions totpOptions;

	private ExecutorService executorService;
	
	@Override
	public ExecutorService getExecutorService() {
		if(executorService==null) {
			log.debug("Creating DeviceConnectionExecutorService with {} threads", totpOptions.getExecutorServiceThreads());
			executorService = Executors.newFixedThreadPool(totpOptions.getExecutorServiceThreads());
		}
		return executorService;
	}

}
