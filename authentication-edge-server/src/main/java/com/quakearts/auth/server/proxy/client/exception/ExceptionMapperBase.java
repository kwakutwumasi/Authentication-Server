package com.quakearts.auth.server.proxy.client.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;

import com.quakearts.auth.server.proxy.client.model.ErrorResponse;

public abstract class ExceptionMapperBase<T extends Throwable> 
	implements ExceptionMapper<T> {

	public ExceptionMapperBase() {
		super();
	}

	@Override
	public Response toResponse(T exception) {
		return processThrowable(exception);
	}

	protected Response processThrowable(T exception) {
		ErrorResponse errorResponse = new ErrorResponse()
				.withCodeAs(exception.getClass().getName())
				.addExplanation(extractMessage(exception));
		
		Throwable cause = exception.getCause();
		if(cause != null)
			errorResponse.addExplanation(extractMessage(cause));
		
		return createResponse(exception).entity(errorResponse)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.build();
	}

	protected abstract ResponseBuilder createResponse(T exception);

	private String extractMessage(Throwable exception) {
		String message = exception.getMessage();
		return message+""; //In case its null
	}

}