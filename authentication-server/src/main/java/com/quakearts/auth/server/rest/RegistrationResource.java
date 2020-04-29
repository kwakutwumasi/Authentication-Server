package com.quakearts.auth.server.rest;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.infinispan.Cache;

import com.quakearts.auth.server.rest.models.Registration;
import com.quakearts.auth.server.rest.services.ErrorService;
import com.quakearts.auth.server.rest.services.OptionsService;
import com.quakearts.auth.server.rest.validators.annotation.ValidNewRegistration;
import com.quakearts.auth.server.rest.validators.annotation.ValidUpdatedRegistration;
import com.quakearts.auth.server.store.annotation.AliasStore;
import com.quakearts.auth.server.store.annotation.RegistryStore;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Singleton
@Path("registration")
@Produces(MediaType.APPLICATION_JSON)
public class RegistrationResource {
	
	@Inject @RegistryStore
	private Cache<String, Registration> store;
	
	@Inject @AliasStore
	private Cache<String, String> aliases;
	
	@Inject
	private OptionsService optionsService;
	
	@Inject
	private ErrorService errorService;
	
	@Inject
	private AuthenticationResource authenticationResource;
	
	@Operation(summary="List all aliases registered on the server")
	@ApiResponse(responseCode="200",
	 description="The aliases were successfully listed",
	 content=@Content(array=@ArraySchema(schema=@Schema(implementation=String.class))))
	@GET
	public void getAllAliases(@Suspended final AsyncResponse asyncResponse) {
		CompletableFuture.runAsync(()->
			asyncResponse.resume(aliases.keySet())
		);
	}

	private void respondNotFound(final AsyncResponse asyncResponse) {
		asyncResponse.resume(new WebApplicationException(Response.status(Status.NOT_FOUND)
				.entity(errorService.createErrorResponse("invalid-id",
						"A registration with the provided ID could not be found")).build()));
	}
	
	@Operation(summary="Register an application for authentication",
				description="API services that outsource authentication to this server need to configure the "
						+ "login modules to be used in authenticating and authorizing subjects. This interface "
						+ "provides applications and application administrators the ability to configure "
						+ "authentication options. The registration id is sensitive and must not be shared. "
						+ "It is required to manage, update or delete the registration configuration. The alias "
						+ "can be shared, and is used by client applications that want to obtain the necessary "
						+ "authorization tokens for the API services.",
				requestBody=@RequestBody(content=@Content(schema=@Schema(implementation=Registration.class,
																		 description="The registration details",
																		 example="{\n" + 
											 								 		"   \"id\":\"9F86D081884C7D659A2FEAA0C55AD015A3BF4F1B2B0B822CD15D6C15B0F00A08\",\n" + 
											 								 		"   \"alias\":\"test-rest\",\n" + 
											 								 		"   \"configurations\":[\n" + 
											 								 		"      {\n" + 
											 								 		"         \"name\":\"Test\",\n" + 
											 								 		"         \"entries\":[\n" + 
											 								 		"            {\n" + 
											 								 		"               \"moduleClassname\":\"com.quakearts.auth.server.test.TestLoginModule\",\n" + 
											 								 		"               \"moduleFlag\":\"REQUIRED\",\n" + 
											 								 		"               \"options\":{\n" + 
											 								 		"                  \"test\":\"value\"\n" + 
											 								 		"               }\n" + 
											 								 		"            }\n" + 
											 								 		"         ]\n" + 
											 								 		"      }\n" + 
											 								 		"   ],\n" + 
											 								 		"   \"options\":{\n" + 
											 								 		"      \"audience\":\"https://demo.quakearts.com\",\n" + 
											 								 		"      \"validity.period\":\"1 Day\",\n" + 
											 								 		"      \"secret\":\"W@h8237HksIhfmsd2Nl94WNCA\",\n" + 
											 								 		"      \"issuer\":\"https://quakearts.com\"\n" + 
											 								 		"   }\n" + 
											 								 		"}"))))
	@ApiResponse(responseCode="204",
				description="Registration succeeded")
	@ApiResponse(responseCode="400",
				description="The presented registration parameters are not valid",
				content=@Content(schema=@Schema(
						implementation=com.quakearts.auth.server.rest.models.ErrorResponse.class,
				description="A JSON object describing and explaining the error",
				example="{\n" + 
						"    \"code\": \"existing-id\",\n" + 
						"    \"explanations\": [\n" + 
						"        \"A registration with the provided ID/alias already exists\"\n" + 
						"    ]\n" + 
						"}")))
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void register(@ValidNewRegistration @Valid final Registration registration, 
			final @Suspended AsyncResponse asyncResponse) {
		CompletableFuture.runAsync(()->{
			if(store.containsKey(registration.getId())
					|| aliases.containsKey(registration.getAlias())) {
				respondBadRequest(asyncResponse);
			} else {
				storeRegistration(registration, null);
				asyncResponse.resume(Response.noContent().build());
			}
		});
	}

	private void respondBadRequest(AsyncResponse asyncResponse) {
		asyncResponse.resume(new WebApplicationException(Response.status(Status.BAD_REQUEST)
						.entity(errorService.createErrorResponse("existing-id",
								"A registration with the provided ID/alias already exists")).build()));
	}
	
	private void storeRegistration(final Registration registration, final Switcher switcher) {
		registration.setOptions(optionsService.buildOptions(registration.getOptions()));
		store.put(registration.getId(), registration);
		if(switcher!=null) {
			if(switcher.switchAlias) {
				aliases.remove(switcher.oldAlias);
				aliases.put(registration.getAlias(), registration.getId());
			}
		} else {
			aliases.put(registration.getAlias(), registration.getId());
		}
	}
	
	@Operation(summary="Update the registeration",
			requestBody=@RequestBody(content=@Content(schema=@Schema(implementation=Registration.class,
																	 description="The registration details",
																	 example="{\n" + 
										 								 		"   \"alias\":\"test-rest\",\n" + 
										 								 		"   \"configurations\":[\n" + 
										 								 		"      {\n" + 
										 								 		"         \"name\":\"Test\",\n" + 
										 								 		"         \"entries\":[\n" + 
										 								 		"            {\n" + 
										 								 		"               \"moduleClassname\":\"com.quakearts.auth.server.test.TestLoginModule\",\n" + 
										 								 		"               \"moduleFlag\":\"REQUIRED\",\n" + 
										 								 		"               \"options\":{\n" + 
										 								 		"                  \"test\":\"value\"\n" + 
										 								 		"               }\n" + 
										 								 		"            }\n" + 
										 								 		"         ]\n" + 
										 								 		"      }\n" + 
										 								 		"   ],\n" + 
										 								 		"   \"options\":{\n" + 
										 								 		"      \"audience\":\"https://demo.quakearts.com\",\n" + 
										 								 		"      \"validity.period\":\"1 Day\",\n" + 
										 								 		"      \"secret\":\"W@h8237HksIhfmsd2Nl94WNCA\",\n" + 
										 								 		"      \"issuer\":\"https://quakearts.com\"\n" + 
										 								 		"   }\n" + 
										 								 		"}"))))
	@ApiResponse(responseCode="204",
				description="Registration update succeeded")
	@ApiResponse(responseCode="400",
				description="The presented registration parameters are not valid",
				content=@Content(schema=@Schema(
						implementation=com.quakearts.auth.server.rest.models.ErrorResponse.class,
				description="A JSON object describing and explaining the error",
				example="{\n" + 
						"    \"code\": \"existing-id\",\n" + 
						"    \"explanations\": [\n" + 
						"        \"A registration with the provided ID/alias already exists\"\n" + 
						"    ]\n" + 
						"}")))
	@ApiResponse(responseCode="404",
				description="Authentication ID is not valid",
				content=@Content(schema=@Schema(
						implementation=com.quakearts.auth.server.rest.models.ErrorResponse.class,
				description="A JSON object describing and explaining the error",
				example="{\n" + 
						"    \"code\": \"invalid-id\",\n" + 
						"    \"explanations\": [\n" + 
						"        \"A registration with the provided ID could not be found\"\n" + 
						"    ]\n" + 
						"}")))
	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateRegistration(@PathParam("id") final String id,
			@ValidUpdatedRegistration @Valid final Registration registration, 
			final @Suspended AsyncResponse asyncResponse) {
		CompletableFuture.runAsync(()->{
			Registration oldRegistration = store.get(id);
			Switcher switcher = new Switcher();
			if(oldRegistration==null) {
				respondNotFound(asyncResponse);
			} else if(aliases.containsKey(registration.getAlias())
					&& !aliases.get(registration.getAlias()).equals(id)) {
				respondBadRequest(asyncResponse);
			} else {
				prepareParameters(id, registration, oldRegistration, switcher);
				storeRegistration(registration, switcher);
				authenticationResource.resetAuthenticationPack(id);
				asyncResponse.resume(Response.noContent().build());
			}
		});
	}

	private void prepareParameters(final String id, final Registration registration, 
			final Registration oldRegistration, final Switcher switcher) {
		registration.setId(id);
		switcher.oldAlias = oldRegistration.getAlias();
		switcher.switchAlias = !oldRegistration.getAlias()
				.equals(registration.getAlias());
	}
	
	@Operation(summary="Remove the registeration")
	@ApiResponse(responseCode="204",
				description="Registration removal succeeded")
	@ApiResponse(responseCode="404",
				description="Authentication ID is not valid",
				content=@Content(schema=@Schema(
						implementation=com.quakearts.auth.server.rest.models.ErrorResponse.class,
				description="A JSON object describing and explaining the error",
				example="{\n" + 
						"    \"code\": \"invalid-id\",\n" + 
						"    \"explanations\": [\n" + 
						"        \"A registration with the provided ID could not be found\"\n" + 
						"    ]\n" + 
						"}")))	
	@DELETE
	@Path("{id}")
	public void unregister(@PathParam("id")final String id, final @Suspended AsyncResponse asyncResponse) {
		CompletableFuture.runAsync(()->{
			Registration registration = store.remove(id);
			if(registration == null) {
				respondNotFound(asyncResponse);
			} else {
				aliases.remove(registration.getAlias());
				asyncResponse.resume(Response.noContent().build());
			}
		});
	}

	private class Switcher {
		boolean switchAlias;
		String oldAlias;
	}
}
