package com.quakearts.auth.server.totp.rest.model;

public class ManagementResponse {
	private ManagementResponseEntry[] responses;
	
	public ManagementResponseEntry[] getResponses() {
		return responses;
	}
	
	public void setResponses(ManagementResponseEntry[] response) {
		this.responses = response;
	}
}
