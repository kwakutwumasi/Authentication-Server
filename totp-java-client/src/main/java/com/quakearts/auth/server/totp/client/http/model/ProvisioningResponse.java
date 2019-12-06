package com.quakearts.auth.server.totp.client.http.model;

public class ProvisioningResponse {
	private String seed;
	private long initialCounter;

	public String getSeed() {
		return seed;
	}

	public void setSeed(String seed) {
		this.seed = seed;
	}

	public long getInitialCounter() {
		return initialCounter;
	}

	public void setInitialCounter(long initialCounter) {
		this.initialCounter = initialCounter;
	}	
}
