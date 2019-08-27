package com.quakearts.auth.server.totp.rest.authorization.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.auth.server.totp.channel.DeviceConnectionChannel;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.rest.authorization.DeviceAuthorizationService;

@Singleton
public class DeviceAuthorizationServiceImpl implements DeviceAuthorizationService {
	
	@Inject
	private DeviceConnectionChannel deviceConnectionChannel;
	
	@Override
	public void requestOTPCode(String deviceId, Consumer<String> callback, Consumer<String> errorCallback) 
			throws TOTPException {
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("requestType", "otp");
		requestMap.put("deviceId", deviceId);
		deviceConnectionChannel.sendMessage(requestMap, response->{
			String otp = response.get("otp");
			if(otp != null) {				
				callback.accept(otp);
			} else {
				errorCallback.accept(response.get("error"));
			}
		});
	}
}
