package com.quakearts.auth.server.totp.rest;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.quakearts.auth.server.totp.rest.model.SyncResponse;

@Path("sync")
@Singleton
public class SynchronizeResource {
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public SyncResponse synchronize(){
		return new SyncResponse();
	}
}
