package com.quakearts.auth.server.totp.rest.model;

public class SyncResponse {
	private long time = System.currentTimeMillis();
	public long getTime() {
		return time;
	}
}
