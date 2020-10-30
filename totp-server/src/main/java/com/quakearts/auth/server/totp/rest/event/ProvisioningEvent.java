package com.quakearts.auth.server.totp.rest.event;

import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.rest.model.ActivationRequest;

public class ProvisioningEvent {
	private Device device;
	private ActivationRequest activationRequest;
	
	public ProvisioningEvent(Device device, ActivationRequest activationRequest) {
		this.device = device;
		this.activationRequest = activationRequest;
	}
	
	public Device getDevice() {
		return device;
	}
	
	public ActivationRequest getActivationRequest() {
		return activationRequest;
	}
}
