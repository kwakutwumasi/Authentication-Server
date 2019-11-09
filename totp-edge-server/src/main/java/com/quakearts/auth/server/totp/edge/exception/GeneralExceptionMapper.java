package com.quakearts.auth.server.totp.edge.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

public class GeneralExceptionMapper 
	extends ExceptionMapperBase<RuntimeException> {

	@Override
	protected ResponseBuilder createResponse(RuntimeException exception) {
		return Response.serverError();
	}
}
