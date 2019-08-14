package com.quakearts.auth.server.totp.provisioning.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.quakearts.auth.server.totp.provisioning.client.AuthenticationServerHttpClient;
import com.quakearts.auth.server.totp.provisioning.client.exception.ConnectorException;
import com.quakearts.auth.server.totp.provisioning.client.model.ErrorResponse;
import com.quakearts.auth.server.totp.provisioning.client.model.TokenResponse;
import com.quakearts.rest.client.exception.HttpClientException;

@Path("authenticate/{alias}/{application}")
public class AuthenticationResource {

	@Inject
	private AuthenticationServerHttpClient client;
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public TokenResponse provision(@PathParam("alias") String alias,
			@PathParam("application") String application,
			@QueryParam("clientId") String clientId,
			@QueryParam("credential") String credential) {
		try {
			return client.authenticate(alias, application, clientId, credential);
		} catch (ConnectorException e) {
			throw new WebApplicationException(Response.status(e.getHttpCode())
					.entity(e.getResponse()).build());
		} catch (IOException | HttpClientException e) {
			throw new WebApplicationException(Response.serverError().entity(new ErrorResponse()
					.addExplanation(e.getMessage())).build());
		}
	}
	
}
