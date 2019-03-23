package com.quakearts.auth.server.totp.rest.model;

public class TokenResponse {
	private String token;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
	public TokenResponse withTokenAs(String token) {
		setToken(token);
		return this;
	}
}
