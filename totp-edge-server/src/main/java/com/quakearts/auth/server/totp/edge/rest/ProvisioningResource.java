package com.quakearts.auth.server.totp.edge.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.quakearts.auth.server.totp.edge.client.TOTPServerHttpClient;
import com.quakearts.auth.server.totp.edge.client.model.ActivationRequest;
import com.quakearts.auth.server.totp.edge.client.model.ProvisioningResponse;
import com.quakearts.rest.client.exception.HttpClientException;

@Path("provisioning/{deviceId}")
@Singleton
public class ProvisioningResource {

	@Inject
	private TOTPServerHttpClient client;
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public ProvisioningResponse provision(@PathParam("deviceId") String deviceId) 
			throws IOException, HttpClientException {
		return client.provision(deviceId);
	}
	
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void activate(@PathParam("deviceId") String deviceId, ActivationRequest request) 
			throws IOException, HttpClientException {
		client.activate(deviceId, request);
	}
}
