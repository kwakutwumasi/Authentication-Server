package com.quakearts.auth.server.totp.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import com.quakearts.auth.server.totp.rest.model.ErrorResponse;
import com.quakearts.webapp.orm.exception.DataStoreException;

public class DataStoreExceptionMapper implements ExceptionMapper<DataStoreException> {

	@Override
	public Response toResponse(DataStoreException exception) {
		return Response.status(Status.EXPECTATION_FAILED)
				.entity(new ErrorResponse()
						.withMessageAs(exception.getMessage()
								+(exception.getCause()!=null?exception.getCause().getMessage():"")))
				.type(MediaType.APPLICATION_JSON_TYPE)
				.build();
	}

}
