package com.quakearts.auth.server.totp.rest;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.quakearts.auth.server.totp.rest.model.SyncResponse;

@Path("sync")
@Singleton
public class SynchronizeResource {
	@GET
	public SyncResponse synchronize(){
		return new SyncResponse();
	}
}
