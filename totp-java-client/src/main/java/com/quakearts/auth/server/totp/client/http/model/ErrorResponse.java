package com.quakearts.auth.server.totp.client.http.model;

import java.io.Serializable;

public class ErrorResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2929262283926384403L;
	private String message;
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public ErrorResponse withMessageAs(String message) {
		setMessage(message);
		return this;
	}

}
