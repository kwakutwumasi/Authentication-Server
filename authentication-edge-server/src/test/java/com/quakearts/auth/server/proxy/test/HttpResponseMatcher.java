package com.quakearts.auth.server.proxy.test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import com.quakearts.auth.server.proxy.client.exception.ConnectorException;
import com.quakearts.auth.server.proxy.client.model.ErrorResponse;

class HttpResponseMatcher 
	extends BaseMatcher<ConnectorException> {

	/**
	 * 
	 */
	ErrorResponse errorResponse;
	int httpCode;

	HttpResponseMatcher(ErrorResponse errorResponse, int httpCode) {
		this.errorResponse = errorResponse;
		this.httpCode = httpCode;
	}

	@Override
	public boolean matches(Object item) {
		ConnectorException exception = (ConnectorException) item;
		return exception.getHttpCode() == httpCode && 
				errorResponse.getCode().equals(exception.getResponse().getCode()) &&
				errorResponse.getExplanations().equals(exception.getResponse().getExplanations());
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("http code: "+httpCode+", error response code: "
				+errorResponse.getCode()+", explanations: "
				+errorResponse.getExplanations());
	}
	
}