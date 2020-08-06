package com.quakearts.auth.server.totp.rest.model;

import java.util.Map;

public class DirectAuthenticationRequest {

	private String deviceId;
	private Map<String, String> authenticationData;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	public Map<String, String> getAuthenticationData() {
		return authenticationData;
	}

	public void setAuthenticationData(Map<String, String> authenticationData) {
		this.authenticationData = authenticationData;
	}
}
