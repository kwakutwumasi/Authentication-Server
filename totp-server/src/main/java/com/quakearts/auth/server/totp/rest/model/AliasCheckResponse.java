package com.quakearts.auth.server.totp.rest.model;

public class AliasCheckResponse {
	private boolean active;
	private boolean connected;
	
	public boolean isActive() {
		return active;
	}
	
	public AliasCheckResponse withActiveAs(boolean active) {
		this.active = active;
		return this;
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public AliasCheckResponse withConnectedAs(boolean connected) {
		this.connected = connected;
		return this;
	}
}
