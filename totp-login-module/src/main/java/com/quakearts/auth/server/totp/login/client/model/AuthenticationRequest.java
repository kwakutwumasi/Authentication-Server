package com.quakearts.auth.server.totp.login.client.model;

public class AuthenticationRequest {
	private String deviceId;
	private String otp;

	public String getDeviceId() {
		return deviceId;
	}

	public AuthenticationRequest setDeviceIdAs(String deviceId) {
		this.deviceId = deviceId;
		return this;
	}

	public String getOtp() {
		return otp;
	}

	public AuthenticationRequest setOtpAs(String otp) {
		this.otp = otp;
		return this;
	}

}
