package com.quakearts.auth.server.totp.client.http.model;

public class ActivationRequest {
	private String token;
	private String alias;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
	
}
