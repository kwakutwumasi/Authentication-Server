package com.quakearts.auth.server.totp.edge.exception;

import java.text.MessageFormat;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;

import com.quakearts.auth.server.totp.edge.client.model.ErrorResponse;

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
		StringBuilder errorBuilder = new StringBuilder(extractMessage(exception));
		
		Throwable cause = exception.getCause();
		if(cause != null)
			errorBuilder.append("\n").append(extractMessage(cause));
		
		return createResponse(exception).entity(new ErrorResponse()
				.withMessageAs(errorBuilder.toString()))
				.type(MediaType.APPLICATION_JSON_TYPE)
				.build();
	}

	protected abstract ResponseBuilder createResponse(T exception);

	private String extractMessage(Throwable exception) {
		String message = exception.getMessage();
		
		if(message == null)
			message = MessageFormat.format("Exception of type {0}", exception.getClass().getName());
		return message;
	}

}