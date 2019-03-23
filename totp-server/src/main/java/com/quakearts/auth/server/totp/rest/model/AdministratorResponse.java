package com.quakearts.auth.server.totp.rest.model;

import com.quakearts.auth.server.totp.model.Administrator;

public class AdministratorResponse {
	private String deviceId;
	private String commonName;
	
	public AdministratorResponse() {}
	
	public AdministratorResponse(Administrator administrator) {
		deviceId = administrator.getDevice().getId();
		commonName = administrator.getCommonName();
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getCommonName() {
		return commonName;
	}

	public void setCommonName(String alias) {
		this.commonName = alias;
	}
}
