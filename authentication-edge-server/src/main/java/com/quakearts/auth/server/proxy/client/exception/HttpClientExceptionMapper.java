package com.quakearts.auth.server.proxy.client.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.quakearts.rest.client.exception.HttpClientException;

public class HttpClientExceptionMapper
	extends ExceptionMapperBase<HttpClientException> {

	@Override
	protected ResponseBuilder createResponse(HttpClientException exception) {
		return Response.status(500);
	}

}
