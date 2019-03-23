package com.quakearts.auth.server.totp.rest.model;

import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;

public class DeviceResponse {
	
	private String deviceId;
	private Status status;
	private long itemCount;
	
	public DeviceResponse(Device device) {
		deviceId = device.getId();
		status = device.getStatus();
		itemCount = device.getItemCount();
	}

	public DeviceResponse() {}
	
	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public long getItemCount() {
		return itemCount;
	}

	public void setItemCount(long itemCount) {
		this.itemCount = itemCount;
	}

}
