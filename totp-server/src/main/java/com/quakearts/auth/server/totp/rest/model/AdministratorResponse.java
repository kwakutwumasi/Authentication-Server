package com.quakearts.auth.server.totp.rest.model;

import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.auth.server.totp.model.Device.Status;

public class AdministratorResponse {
	private String deviceId;
	private Status deviceStatus;
	private String commonName;
	
	public AdministratorResponse() {}
	
	public AdministratorResponse(Administrator administrator) {
		deviceId = administrator.getDevice().getId();
		deviceStatus = administrator.getDevice().getStatus();
		commonName = administrator.getCommonName();
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public Status getDeviceStatus() {
		return deviceStatus;
	}

	public void setDeviceStatus(Status deviceStatus) {
		this.deviceStatus = deviceStatus;
	}

	public String getCommonName() {
		return commonName;
	}

	public void setCommonName(String alias) {
		this.commonName = alias;
	}
}
