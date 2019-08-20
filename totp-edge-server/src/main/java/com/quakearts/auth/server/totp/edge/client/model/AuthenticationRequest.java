package com.quakearts.auth.server.totp.edge.client.model;

public class AuthenticationRequest {
	private String deviceId;
	private String otp;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getOtp() {
		return otp;
	}

	public void setOtp(String otp) {
		this.otp = otp;
	}

}
