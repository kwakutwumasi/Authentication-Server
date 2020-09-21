package com.quakearts.auth.server.rest;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.rest.models.Secret;
import com.quakearts.auth.server.store.annotation.SecretsStore;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("secrets")
@Produces(MediaType.APPLICATION_JSON)
public class SecretsResource {
	
	@Inject @SecretsStore
	private Cache<String, String> secretStore;
	
	private static final Logger log = LoggerFactory.getLogger(SecretsResource.class);
	
	@Operation(tags = OpenApiDefinition.REGISTRATION_ENDPOINTS, summary="Store a secret key and value",
				requestBody=@RequestBody(content=
								@Content(schema=
									@Schema(description="The secret key and value", 
											example="{\n" + 
													"	\"key\":\"{database.password}\",\n" + 
													"	\"value\":\"#$dfskKJI3@##$2dfsd\"\n" + 
													"}"))))
	@ApiResponse(responseCode="204", 
		description="Request was successful")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void addSecret(@NotNull @Valid final Secret secret, @Suspended final AsyncResponse asyncResponse) {
		CompletableFuture.runAsync(()->{
			log.trace("Loading secret key {}", secret.getKey());
			secretStore.put(secret.getKey(), secret.getValue());
			asyncResponse.resume(Response.noContent().build());
			log.trace("Loaded secret key {}", secret.getKey());
		});
	}
	
	@Operation(tags = OpenApiDefinition.REGISTRATION_ENDPOINTS, summary="Remove a secret key and value")
	@ApiResponse(responseCode="204", 
		description="Request was successful")
	@DELETE
	@Path("{key}")
	public void removeKey(@PathParam("key") final String key, @Suspended final AsyncResponse asyncResponse) {
		CompletableFuture.runAsync(()->{
			log.trace("Removing secret key {}", key);
			secretStore.remove(key);
			asyncResponse.resume(Response.noContent().build());	
			log.trace("Removed secret key {}", key);
		});
	}
}
