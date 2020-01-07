package com.quakearts.auth.server.totp.rest.model;

import java.util.Set;
import java.util.stream.Collectors;

import com.quakearts.auth.server.totp.model.Alias;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;

public class DeviceResponse {
	
	private String deviceId;
	private Status status;
	private long itemCount;
	private Set<String> aliases;
	
	public DeviceResponse(Device device) {
		deviceId = device.getId();
		status = device.getStatus();
		itemCount = device.getItemCount();
		aliases = device.getAliases()
				.stream().map(Alias::getName)
				.collect(Collectors.toSet());
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
	
	public Set<String> getAliases() {
		return aliases;
	}
	
	public void setAliases(Set<String> aliases) {
		this.aliases = aliases;
	}
}
