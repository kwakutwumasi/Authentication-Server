package com.quakearts.auth.server.totp.edge.exception;

import java.io.IOException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

public class IOExceptionMapper extends ExceptionMapperBase<IOException> {

	@Override
	protected ResponseBuilder createResponse(IOException exception) {
		return Response.status(504);
	}
}
