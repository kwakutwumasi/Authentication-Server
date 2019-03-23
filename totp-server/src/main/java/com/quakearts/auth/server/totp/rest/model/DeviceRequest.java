package com.quakearts.auth.server.totp.rest.model;

public class DeviceRequest {
	private String deviceId;
	private String alias;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

}
