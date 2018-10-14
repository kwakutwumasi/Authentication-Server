package com.quakearts.auth.server.exeptionmapper;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import com.quakearts.auth.server.rest.models.ErrorResponse;

public class ValidationExceptionMapper 
	implements ExceptionMapper<ConstraintViolationException> {

	@Override
	public Response toResponse(ConstraintViolationException exception) {
		ErrorResponse errorResponse = new ErrorResponse().setCodeAs("invalid-data");
		for(ConstraintViolation<?> violation:exception.getConstraintViolations()) {
			errorResponse.addExplanation(violation.getMessage());
		}
		return Response.status(Status.BAD_REQUEST)
				.type(MediaType.APPLICATION_JSON)
				.entity(errorResponse).build();
	}

}
