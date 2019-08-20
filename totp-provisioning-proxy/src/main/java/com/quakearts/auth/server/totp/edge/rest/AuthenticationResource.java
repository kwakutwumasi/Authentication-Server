package com.quakearts.auth.server.totp.edge.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.quakearts.auth.server.totp.edge.client.TOTPServerHttpClient;
import com.quakearts.auth.server.totp.edge.client.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.edge.client.model.ErrorResponse;
import com.quakearts.auth.server.totp.edge.exception.ConnectorException;
import com.quakearts.rest.client.exception.HttpClientException;

@Path("authenticate")
@Singleton
public class AuthenticationResource {

	@Inject
	private TOTPServerHttpClient client;
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public void authentication(AuthenticationRequest request) {
		try {
			client.authentication(request);
		} catch (ConnectorException e) {
			throw new WebApplicationException(Response.status(e.getHttpCode())
					.entity(e.getResponse()).build());
		} catch (IOException | HttpClientException e) {
			throw new WebApplicationException(Response.serverError().entity(new ErrorResponse()
					.withMessageAs(e.getMessage())).build());
		}
	}
	
}
