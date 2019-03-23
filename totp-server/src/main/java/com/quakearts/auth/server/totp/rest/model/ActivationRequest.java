package com.quakearts.auth.server.totp.rest.model;

public class ActivationRequest {
	private String token;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
