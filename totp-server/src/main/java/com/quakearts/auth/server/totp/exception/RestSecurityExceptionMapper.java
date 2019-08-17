package com.quakearts.auth.server.totp.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.quakearts.auth.server.totp.rest.model.ErrorResponse;
import com.quakearts.webapp.security.rest.exception.RestSecurityException;

public class RestSecurityExceptionMapper implements ExceptionMapper<RestSecurityException> {

	@Override
	public Response toResponse(RestSecurityException exception) {
		return Response.status(403)
				.entity(new ErrorResponse()
						.withMessageAs(exception.getMessage()))
				.build();
	}

}
