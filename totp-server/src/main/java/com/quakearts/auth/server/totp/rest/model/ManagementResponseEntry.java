package com.quakearts.auth.server.totp.rest.model;

public class ManagementResponseEntry {
	private boolean error;
	private String message;

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public ManagementResponseEntry withErrorAs(boolean error) {
		setError(error);
		return this;
	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public ManagementResponseEntry withMessageAs(String message) {
		setMessage(message);
		return this;
	}
}
