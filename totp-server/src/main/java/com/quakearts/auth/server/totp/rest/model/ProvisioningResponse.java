package com.quakearts.auth.server.totp.rest.model;

public class ProvisioningResponse {
	private String seed;
	private long initialCounter;

	public String getSeed() {
		return seed;
	}

	public void setSeed(String seed) {
		this.seed = seed;
	}

	public ProvisioningResponse withSeedAs(String seed) {
		setSeed(seed);
		return this;
	}
	
	public long getInitialCounter() {
		return initialCounter;
	}

	public void setInitialCounter(long initialCounter) {
		this.initialCounter = initialCounter;
	}
	
	public ProvisioningResponse withInitialCounterAs(long initialCounter) {
		setInitialCounter(initialCounter);
		return this;
	}
}
