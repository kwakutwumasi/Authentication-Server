package com.quakearts.auth.server.totp.edge.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class ConnectorExceptionMapper implements ExceptionMapper<ConnectorException> {

	@Override
	public Response toResponse(ConnectorException exception) {
		return Response.status(exception.getHttpCode())
				.entity(exception.getResponse())
				.type(MediaType.APPLICATION_JSON)
				.build();
	}

}
