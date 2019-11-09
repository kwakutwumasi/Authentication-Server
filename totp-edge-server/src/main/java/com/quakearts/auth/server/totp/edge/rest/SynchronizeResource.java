package com.quakearts.auth.server.totp.edge.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.quakearts.auth.server.totp.edge.client.TOTPServerHttpClient;
import com.quakearts.auth.server.totp.edge.client.model.SyncResponse;
import com.quakearts.rest.client.exception.HttpClientException;

@Path("sync")
@Singleton
public class SynchronizeResource {
	
	@Inject
	private TOTPServerHttpClient client;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public SyncResponse synchronize() 
			throws IOException, HttpClientException{
		return client.synchronize();
	}
}
