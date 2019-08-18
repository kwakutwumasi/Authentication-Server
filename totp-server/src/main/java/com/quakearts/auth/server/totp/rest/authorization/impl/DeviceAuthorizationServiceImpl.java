package com.quakearts.auth.server.totp.rest.authorization.impl;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.auth.server.totp.channel.DeviceConnectionChannel;
import com.quakearts.auth.server.totp.exception.MessageGenerationException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.rest.authorization.DeviceConnectionService;

@Singleton
public class DeviceConnectionServiceImpl implements DeviceConnectionService {
	
	@Inject
	private DeviceConnectionChannel deviceConnectionChannel;
	
	@Override
	public String requestOTPCode(String deviceId) throws UnconnectedDeviceException,
		MessageGenerationException {
		Map<String, String> requestMap = new HashMap<>();
		requestMap.put("requestType", "otp");
		requestMap.put("deviceId", deviceId);
		Map<String, String> response = deviceConnectionChannel.sendMessage(requestMap);
		String otp = response.get("otp");
		if(otp == null) {
			throw new UnconnectedDeviceException("Response was not undestood");
		}
		return otp;
	}
}
