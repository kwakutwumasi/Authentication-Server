package com.quakearts.auth.server.totp.rest.authorization.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.channel.DeviceConnectionChannel;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.rest.authorization.DeviceAuthorizationService;

@Singleton
public class DeviceAuthorizationServiceImpl implements DeviceAuthorizationService {
	
	private static final Logger log = LoggerFactory.getLogger(DeviceAuthorizationService.class);
			
	@Inject
	private DeviceConnectionChannel deviceConnectionChannel;
	
	@Override
	public void requestOTPCode(Device device, Map<String, String> authenticationData, 
			Consumer<String> callback, Consumer<String> errorCallback) throws TOTPException {
		Map<String, String> requestMap = new HashMap<>();
		if(authenticationData!=null && !authenticationData.isEmpty())
			requestMap.putAll(authenticationData);

		requestMap.put("requestType", "otp");
		requestMap.put("deviceId", device.getId());
		
		log.debug("Sending otp code request message with hashCode: {} for device with itemCount: {}", 
				requestMap.hashCode(), device.getItemCount());
		
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
