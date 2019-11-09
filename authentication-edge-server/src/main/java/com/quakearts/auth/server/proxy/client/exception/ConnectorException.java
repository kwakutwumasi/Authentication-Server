package com.quakearts.auth.server.proxy.client.exception;

import com.quakearts.auth.server.proxy.client.model.ErrorResponse;
import com.quakearts.rest.client.exception.HttpClientException;

public class ConnectorException extends HttpClientException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1168001922075034516L;

	private final ErrorResponse response;
	private final int httpCode;
	
	public ConnectorException(ErrorResponse response, int httpCode) {
		super("Connection error");
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
