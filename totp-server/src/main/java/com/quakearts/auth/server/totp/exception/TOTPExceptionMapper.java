package com.quakearts.auth.server.totp.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.quakearts.auth.server.totp.rest.model.ErrorResponse;

public class TOTPExceptionMapper implements ExceptionMapper<TOTPException> {

	@Override
	public Response toResponse(TOTPException exception) {
		return Response.status(exception.getHttpCode())
				.entity(new ErrorResponse()
						.withMessageAs(exception.getMessage()))
				.build();
	}
}
