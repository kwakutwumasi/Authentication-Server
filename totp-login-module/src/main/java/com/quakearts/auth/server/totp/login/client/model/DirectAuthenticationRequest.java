package com.quakearts.auth.server.totp.login.client.model;

import java.util.HashMap;
import java.util.Map;

public class DirectAuthenticationRequest {
	private String deviceId;
	private Map<String, String> authenticationData;

	public String getDeviceId() {
		return deviceId;
	}

	public DirectAuthenticationRequest setDeviceIdAs(String deviceId) {
		this.deviceId = deviceId;
		return this;
	}
	
	public Map<String, String> getAuthenticationData() {
		return authenticationData;
	}
	
	public DirectAuthenticationRequest addAuthenticationData(String key, String value) {
		if(authenticationData == null)
			authenticationData = new HashMap<>();
		authenticationData.put(key, value);
		return this;
	}
}
