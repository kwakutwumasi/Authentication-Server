package com.quakearts.auth.server.totp.edge.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.quakearts.auth.server.totp.edge.client.TOTPServerHttpClient;
import com.quakearts.auth.server.totp.edge.client.model.AuthenticationRequest;
import com.quakearts.rest.client.exception.HttpClientException;

@Path("authenticate")
@Singleton
public class AuthenticationResource {

	@Inject
	private TOTPServerHttpClient client;
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public void authentication(AuthenticationRequest request) 
			throws IOException, HttpClientException {
		client.authentication(request);
	}
	
}
