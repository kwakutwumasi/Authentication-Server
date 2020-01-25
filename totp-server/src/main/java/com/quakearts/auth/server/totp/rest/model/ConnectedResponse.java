package com.quakearts.auth.server.totp.rest.model;

public class ConnectedResponse {
	private boolean connected;

	public boolean isConnected() {
		return connected;
	}

	public ConnectedResponse withConnectedAs(boolean connected) {
		this.connected = connected;
		return this;
	}
}
