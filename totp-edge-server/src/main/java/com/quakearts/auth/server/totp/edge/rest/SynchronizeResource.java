package com.quakearts.auth.server.totp.edge.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.quakearts.auth.server.totp.edge.client.TOTPServerHttpClient;
import com.quakearts.auth.server.totp.edge.client.model.ErrorResponse;
import com.quakearts.auth.server.totp.edge.client.model.SyncResponse;
import com.quakearts.auth.server.totp.edge.exception.ConnectorException;
import com.quakearts.rest.client.exception.HttpClientException;

@Path("sync")
@Singleton
public class SynchronizeResource {
	
	@Inject
	private TOTPServerHttpClient client;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public SyncResponse synchronize(){
		try {
			return client.synchronize();
		} catch (IOException | HttpClientException e) {
			throw new WebApplicationException(Response.serverError().entity(new ErrorResponse()
					.withMessageAs(e.getMessage())).build());
		} catch (ConnectorException e) {
			throw new WebApplicationException(Response.status(e.getHttpCode())
					.entity(e.getResponse()).build());
		}
	}
}
