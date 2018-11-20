package com.quakearts.auth.tests.authentication.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;

import org.infinispan.Cache;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.rest.AuthenticationResource;
import com.quakearts.auth.server.rest.models.ErrorResponse;
import com.quakearts.auth.server.rest.models.Registration;
import com.quakearts.auth.server.rest.models.TokenResponse;
import com.quakearts.auth.server.rest.models.LoginConfigurationEntry.ModuleFlag;
import com.quakearts.auth.server.store.annotation.AliasStore;
import com.quakearts.auth.server.store.annotation.RegistryStore;
import com.quakearts.auth.tests.authentication.loginmodule.TestLoginModule;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.factory.JWTFactory;
import com.quakearts.webapp.security.jwt.signature.HMac;

@RunWith(MainRunner.class)
public class AuthenticationResourceTest {

	@Inject
	private AuthenticationResource authenticationResource;
	private final ArrayBlockingQueue<TokenResponse> tokenResponses = new ArrayBlockingQueue<>(1);
	private final ArrayBlockingQueue<ErrorResponse> errorResponses = new ArrayBlockingQueue<>(1);
	private AsyncResponse asyncResponse;
	
	@Rule
	public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
	
	@Inject @RegistryStore
	Cache<String, Registration> store;
	@Inject @AliasStore
	Cache<String, String> aliases;
	
	@Before
	public void createCacheItems() {
		if(!store.containsKey("66F96690A81BF8296FE7BF8BD56E3D10FA0A27AF87B709C9F4BF62266DA7800F")) {
			store.put("66F96690A81BF8296FE7BF8BD56E3D10FA0A27AF87B709C9F4BF62266DA7800F", new Registration()
					.withIdAs("66F96690A81BF8296FE7BF8BD56E3D10FA0A27AF87B709C9F4BF62266DA7800F")
					.withAliasAs("test-main")
					.createConfiguration()
						.setNameAs("Test")
							.createEntry()
							.setModuleClassnameAs(TestLoginModule.class.getName())
							.setModuleFlagAs(ModuleFlag.REQUIRED)
						.addOption("anoption", "value")
						.thenAdd()
					.thenAdd()
					.addOption("algorithm", HMac.HSAlgorithmType.HS256.toString())
					.addOption("secret", "7275C3CA5AE70D9226FA5")
					.addOption("issuer", "https://quakearts.com")
					.addOption("audience", "https://quakearts.com")
					.addOption("validity.period", "15 Minutes")
					.addOption("grace.period", "1"));
	
			aliases.put("test-main", "66F96690A81BF8296FE7BF8BD56E3D10FA0A27AF87B709C9F4BF62266DA7800F");
		}
		
		if(!store.containsKey("05F01D5F71DAD94DB2F136027275C3CA5AE70D9226FA580A58EEAFB33A1C2FF9")) {
			store.put("05F01D5F71DAD94DB2F136027275C3CA5AE70D9226FA580A58EEAFB33A1C2FF9", new Registration()
				.withIdAs("05F01D5F71DAD94DB2F136027275C3CA5AE70D9226FA580A58EEAFB33A1C2FF9")
				.withAliasAs("test-options")
				.createConfiguration()
					.setNameAs("Test")
						.createEntry()
						.setModuleClassnameAs(TestLoginModule.class.getName())
						.setModuleFlagAs(ModuleFlag.REQUIRED)
					.addOption("anoption", "value")
					.thenAdd()
				.thenAdd()
				.addOption("algorithm", HMac.HSAlgorithmType.HS256.toString())
				.addOption("secret", "7275C3CA5AE70D9226FA5")
				.addOption("issuer", "https://mycashbagg.com")
				.addOption("audience", "https://quakearts.com")
				.addOption("validity.period", "15 Minutes")
				.addOption("grace.period", "1"));
			aliases.put("test-options", "05F01D5F71DAD94DB2F136027275C3CA5AE70D9226FA580A58EEAFB33A1C2FF9");
		}
	}
	
	@Test
	public void testAuthenticate() throws Exception {
		
		tokenResponses.clear();
		AsyncResponse response = getOrCreateAsyncResponse();

		authenticationResource.authenticate("test-main","Test", "test", "dGVzdDE=", response);
		TokenResponse tokenResponse = tokenResponses.take();
		assertThat(tokenResponse.getIdToken(), is(notNullValue()));
		assertThat(tokenResponse.getExpiresIn(), is(notNullValue()));
		assertThat(tokenResponse.getTokenType(), is(notNullValue()));
	}

	@Test
	public void testAuthenticateWithOptions() throws Exception {
		tokenResponses.clear();
		AsyncResponse response = getOrCreateAsyncResponse();

		authenticationResource.authenticate("test-options", "Test", "test", "dGVzdDE=", response);
		TokenResponse tokenResponse = tokenResponses.take();
		assertThat(tokenResponse.getIdToken(), is(notNullValue()));
		assertThat(tokenResponse.getExpiresIn(), is(notNullValue()));
		assertThat(tokenResponse.getTokenType(), is(notNullValue()));
		
		String[] tokenParts = tokenResponse.getIdToken().split("\\.",3);
		assertThat(tokenParts.length, is(3));
		JWTClaims claims = JWTFactory.getInstance().createEmptyClaims();
		claims.unCompact(tokenParts[1]);
		assertThat(claims.getIssuer(), is("https://mycashbagg.com"));
	}

	private AsyncResponse getOrCreateAsyncResponse() {
		if(asyncResponse == null)
			asyncResponse = MockingProxyBuilder.createMockingInvocationHandlerFor(AsyncResponse.class)
					.mock("resume(Throwable)").with(arguments->{
						WebApplicationException exception = arguments.get(0);
						errorResponses.put((ErrorResponse) exception.getResponse().getEntity());
						return true;
					})
					.mock("resume(Object)").with(arguments->{
						tokenResponses.put(arguments.get(0));
						return true;
					})
					.thenBuild();
		
		return asyncResponse;
	}
	
	@Test
	public void testAuthenticateInvalid() throws Exception {
		errorResponses.clear();		
		AsyncResponse response = getOrCreateAsyncResponse();
		authenticationResource.authenticate("test-main", "Test", "test", "test2", response);
		ErrorResponse actualErrorResponse = errorResponses.take();
		assertThat(actualErrorResponse.getCode(), is("invalid-credentials"));
		assertThat(actualErrorResponse.getExplanations().contains("The provided credentials could not be authenticated"), is(true));
	}
	
	@Test
	public void testAuthenticateInvalidRegistrationId() throws Exception {
		errorResponses.clear();		
		AsyncResponse response = getOrCreateAsyncResponse();

		authenticationResource.authenticate("test-error-id", "Test", "test", "test2", response);
		ErrorResponse actualErrorResponse = errorResponses.take();
		assertThat(actualErrorResponse.getCode(), is("invalid-id"));
		assertThat(actualErrorResponse.getExplanations().contains("A registration with the provided ID could not be found"), is(true));
	}

}
