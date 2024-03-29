package com.quakearts.auth.server.totp.edge.exception;

import com.quakearts.auth.server.totp.client.http.model.ErrorResponse;

public class ConnectorException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1168001922075034516L;

	private final ErrorResponse response;
	private final int httpCode;
	
	public ConnectorException(ErrorResponse response, int httpCode) {
		this.response = response;
		this.httpCode = httpCode;
	}
	
	public ErrorResponse getResponse() {
		return response;
	}
	
	public int getHttpCode() {
		return httpCode;
	}
}
