package com.quakearts.auth.server.totp.provisioning.client.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.quakearts.auth.server.totp.provisioning.client.model.ErrorResponse;

public class GeneralExceptionMapper implements ExceptionMapper<RuntimeException> {

	@Override
	public Response toResponse(RuntimeException exception) {
		return Response.serverError()
				.entity(new ErrorResponse()
						.withCodeAs("general-error")
						.addExplanation(exception.getMessage()
								+(exception.getCause()!=null?exception.getCause().getMessage():"")))
				.build();
	}

}
