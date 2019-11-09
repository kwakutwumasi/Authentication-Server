package com.quakearts.auth.server.proxy.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.quakearts.auth.server.proxy.client.AuthenticationServerHttpClient;
import com.quakearts.auth.server.proxy.client.model.TokenResponse;
import com.quakearts.rest.client.exception.HttpClientException;

@Path("authenticate/{alias}/{application}")
public class AuthenticationResource {

	@Inject
	private AuthenticationServerHttpClient client;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public TokenResponse provision(@PathParam("alias") String alias,
			@PathParam("application") String application,
			@QueryParam("clientId") String clientId,
			@QueryParam("credential") String credential) throws IOException, HttpClientException {
		return client.authenticate(alias, application, nullCheck(clientId, "clientId"), nullCheck(credential, "credential"));
	}
	
	private String nullCheck(String parameter, String name){
		if(parameter == null)
			throw new IllegalArgumentException(name+" is required");
			
		return parameter;
	}
}
