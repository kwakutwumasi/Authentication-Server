package com.quakearts.auth.server.totp.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.quakearts.auth.server.totp.rest.model.ErrorResponse;

public class GeneralExceptionMapper implements ExceptionMapper<RuntimeException> {

	@Override
	public Response toResponse(RuntimeException exception) {
		return Response.serverError()
				.entity(new ErrorResponse()
						.withMessageAs(exception.getMessage()
								+(exception.getCause()!=null?exception.getCause().getMessage():"")))
				.build();
	}

}
