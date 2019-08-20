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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.quakearts.auth.server.totp.edge.client.TOTPServerHttpClient;
import com.quakearts.auth.server.totp.edge.client.model.ActivationRequest;
import com.quakearts.auth.server.totp.edge.client.model.ErrorResponse;
import com.quakearts.auth.server.totp.edge.client.model.ProvisioningResponse;
import com.quakearts.auth.server.totp.edge.exception.ConnectorException;
import com.quakearts.rest.client.exception.HttpClientException;

@Path("provisioning/{deviceId}")
@Singleton
public class ProvisioningResource {

	@Inject
	private TOTPServerHttpClient client;
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public ProvisioningResponse provision(@PathParam("deviceId") String deviceId) {
		try {
			return client.provision(deviceId);
		} catch (ConnectorException e) {
			throw new WebApplicationException(Response.status(e.getHttpCode())
					.entity(e.getResponse()).build());
		} catch (IOException | HttpClientException e) {
			throw new WebApplicationException(Response.serverError().entity(new ErrorResponse()
					.withMessageAs(e.getMessage())).build());
		}
	}
	
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void activate(@PathParam("deviceId") String deviceId, ActivationRequest request) {
		try {
			client.activate(deviceId, request);
		} catch (ConnectorException e) {
			throw new WebApplicationException(Response.status(e.getHttpCode())
					.type(MediaType.APPLICATION_JSON_TYPE)
					.entity(e.getResponse())
					.build());
		} catch (IOException | HttpClientException e) {
			throw new WebApplicationException(Response.serverError()
					.type(MediaType.APPLICATION_JSON_TYPE)
					.entity(new ErrorResponse()
					.withMessageAs(e.getMessage())).build());
		}
	}
}
