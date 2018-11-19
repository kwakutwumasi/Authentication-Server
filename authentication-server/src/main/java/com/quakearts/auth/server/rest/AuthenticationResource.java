package com.quakearts.auth.server.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.infinispan.Cache;

import com.quakearts.auth.server.rest.models.AuthenticationPack;
import com.quakearts.auth.server.rest.models.Registration;
import com.quakearts.auth.server.rest.models.TokenResponse;
import com.quakearts.auth.server.rest.services.ErrorService;
import com.quakearts.auth.server.store.annotation.AliasStore;
import com.quakearts.auth.server.store.annotation.RegistryStore;
import com.quakearts.webapp.security.auth.JWTLoginModule;
import com.quakearts.webapp.security.auth.JWTPrincipal;
import com.quakearts.webapp.security.auth.UserPrincipal;
import com.quakearts.webapp.security.auth.callback.TokenCallback;
import com.quakearts.webapp.security.rest.context.LoginContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Path("authenticate")
public class AuthenticationResource {
	private final Map<String, AuthenticationPack> 
		authenticationPackCache = new ConcurrentHashMap<>();
	
	@Inject @RegistryStore
	private Cache<String, Registration> store;
		
	@Inject @AliasStore
	private Cache<String, String> aliases;
	
	@Inject
	private ErrorService errorService;
	
	@Operation(summary="Generate a JWT token for authenticating to a server", 
			description="Clients that need an authorization token for API services call this method with "
					+ "credentials. If authenticated, an authorization token is generated and returned. "
					+ "The token will contain the necessary information required by the API services to "
					+ "verify and authorize access to the API methods.")
	@ApiResponse(responseCode="200",
				description="Authentication succeeded",
				content=@Content(schema=@Schema(implementation=TokenResponse.class,
												description="A JSON object containing the JWT token for authorization",
												example="{\n" + 
														"    \"tokenType\": \"bearer\",\n" + 
														"    \"expiresIn\": 86400,\n" + 
														"    \"idToken\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MzkyODgyNDEsImF1ZCI6Imh0dHBzOi8vZGVtby5xdWFrZWFydHMuY29tIiwiaXNzIjoiaHR0cHM6Ly9iYWNrb2ZmaWNlLmIxYWZyaWNhLmNvbSIsImV4cCI6MTUzOTM3NDY0MSwic3ViIjoidGVzdCIsInNhbWFjY291bnRuYW1lIjoidGVzdCIsInRlc3QiOiJ2YWx1ZSJ9.WekqQ9Q2j8I9ZwrswZfJmBLRdxhY5oDjFIiLEaKbGS4\"\n" + 
														"}")))
	@ApiResponse(responseCode="404",
				description="Authentication alias is not valid",
				content=@Content(schema=@Schema(
						implementation=com.quakearts.auth.server.rest.models.ErrorResponse.class,
				description="A JSON object describing and explaining the error",
				example="{\n" + 
						"    \"code\": \"invalid-id\",\n" + 
						"    \"explanations\": [\n" + 
						"        \"A registration with the provided ID could not be found\"\n" + 
						"    ]\n" + 
						"}")))
	@ApiResponse(responseCode="400",
				description="The presented credentials are not valid",
				content=@Content(schema=@Schema(
						implementation=com.quakearts.auth.server.rest.models.ErrorResponse.class,
				description="A JSON object describing and explaining the error",
				example="{\n" + 
						"    \"code\": \"invalid-credentials\",\n" + 
						"    \"explanations\": [\n" + 
						"        \"The provided credentials could not be authenticated\"\n" + 
						"    ]\n" + 
						"}")))
	@GET
	@Path("{alias}/{application}")
	public void authenticate(@PathParam("alias") final String alias,
			@PathParam("application") final String application,
			@NotNull @QueryParam("clientId") final String clientId, 
			@NotNull @QueryParam("credential") final String credential, 
			@Suspended AsyncResponse asyncResponse) {
		CompletableFuture.runAsync(()->{
			String registrationId = aliases.get(alias);
			if(registrationId == null) {
				respondNotFound(asyncResponse);
			} else {
				Registration registration = store.get(registrationId);
				processAuthentication(application, clientId, credential, asyncResponse, registration);
			}
		});
	}

	private void respondNotFound(AsyncResponse asyncResponse) {
		asyncResponse.resume(new WebApplicationException(Response.status(Status.NOT_FOUND)
				.entity(errorService.createErrorResponse("invalid-id",
						"A registration with the provided ID could not be found")).build()));
	}

	private void processAuthentication(final String application, final String clientId, 
			final String credential, AsyncResponse asyncResponse,
			Registration registration) {
		AuthenticationPack pack = getAuthenticationPack(registration);
		Subject subject = new Subject();
		try {
			authenticateCredentials(application, clientId, credential, pack, subject);
			generateToken(clientId, pack, subject);
			respondWithToken(asyncResponse, pack, subject);
		} catch (LoginException e) {
			respondWithError(asyncResponse, e);
		}
	}

	private AuthenticationPack getAuthenticationPack(Registration registration) {
		AuthenticationPack pack = authenticationPackCache.get(registration.getId());
		if(pack == null) {
			pack = new AuthenticationPack(registration);
			authenticationPackCache.put(registration.getId(), pack);
		}
		return pack;
	}

	private void authenticateCredentials(final String application, final String clientId, 
			final String credential, AuthenticationPack pack, Subject subject)
			throws LoginException {
		LoginContext context = new LoginContext(application, subject, callbacks->{
			handleCallbacks(clientId, credential, callbacks);
		}, pack.getConfiguration());
		context.login();
	}

	private void handleCallbacks(final String clientId, final String credential, 
			Callback[] callbacks) {
		for(Callback callback:callbacks) {
			if (callback instanceof PasswordCallback) {
				handlePasswordCallback(credential, callback);
			} else if (callback instanceof NameCallback) {
				handleNameCallback(clientId, callback);
			} else if (callback instanceof TokenCallback) {
				handleTokenCallback(credential, callback);
			}
		}
	}

	private void handlePasswordCallback(final String credential, Callback callback) {
		PasswordCallback passwordCallback = (PasswordCallback) callback;
		passwordCallback.setPassword(credential.toCharArray());
	}

	private void handleNameCallback(final String clientId, Callback callback) {
		NameCallback nameCallback = (NameCallback) callback;
		nameCallback.setName(clientId);
	}

	private void handleTokenCallback(final String credential, Callback callback) {
		TokenCallback tokenCallback = (TokenCallback) callback;
		tokenCallback.setTokenData(credential.getBytes());
	}
	
	private void generateToken(final String clientId, AuthenticationPack pack, 
			Subject subject) throws LoginException {
		Map<String, Object> sharedState = new HashMap<>();
		sharedState.put("javax.security.auth.login.name", new UserPrincipal(clientId));
		sharedState.put("com.quakearts.LoginOk", Boolean.TRUE);
		
		JWTLoginModule jwtLoginModule = new JWTLoginModule();
		jwtLoginModule.initialize(subject, callbacks->{}, sharedState, 
				pack.getModuleOptions());
		jwtLoginModule.login();
		jwtLoginModule.commit();
	}

	private void respondWithToken(AsyncResponse asyncResponse, AuthenticationPack pack, 
			Subject subject) {
		JWTPrincipal principal = 
				subject.getPrincipals(JWTPrincipal.class)
				.iterator().next();
		
		asyncResponse.resume(new TokenResponse()
				.setIdTokenAs(principal.getName())
				.setTokenTypeAs("bearer")
				.setExpiresInAs(pack.getExpiresIn()));
	}

	private void respondWithError(AsyncResponse asyncResponse, Exception e) {
		asyncResponse.resume(new WebApplicationException(Response.status(Status.BAD_REQUEST)
				.entity(errorService.createErrorResponse("invalid-credentials",
						"The provided credentials could not be authenticated")).build()));
	}

	public void resetAuthenticationPack(String id) {
		if(authenticationPackCache.containsKey(id)) {
			authenticationPackCache.remove(id);
		}
	}
}
