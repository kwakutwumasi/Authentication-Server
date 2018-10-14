package com.quakearts.auth.tests.authentication.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import org.infinispan.Cache;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.rest.SecretsResource;
import com.quakearts.auth.server.rest.models.Secret;
import com.quakearts.auth.server.store.annotation.SecretsStore;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;

@RunWith(MainRunner.class)
public class SecretsResourceTest {

	@Rule
	public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

	private final ArrayBlockingQueue<List<String>> secretKeys = new ArrayBlockingQueue<>(1);
	private final ArrayBlockingQueue<Response> responses = new ArrayBlockingQueue<>(1);
	private AsyncResponse asyncResponse;

	@Inject
	private SecretsResource secretsResource;

	@Inject @SecretsStore
	private Cache<String, String> secrets;

	@After
	public void clearCache() {
		secrets.clear();
	}
	
	@Test
	public void testAddListAllRemoveSecretKeys() throws Exception {
		AsyncResponse asyncResponse = getOrCreateAsyncResponse();
		secretsResource.addSecret(new Secret().setKeyAs("{database.password}")
				.setValueAs("HJsm;w92N(uds-aSsm"), asyncResponse);
		assertThat(responses.take().getStatus(), is(204));
		
		secretsResource.listAllSecretKeys(asyncResponse);
		assertThat(secretKeys.take().contains("{database.password}"), is(true));
		
		secretsResource.removeKey("{database.password}", asyncResponse);
		assertThat(responses.take().getStatus(), is(204));

		secretsResource.listAllSecretKeys(asyncResponse);
		assertThat(secretKeys.take().isEmpty(), is(true));
	}
	
	@Test
	public void testInvalidSecretKey() throws Exception {
		AsyncResponse asyncResponse = getOrCreateAsyncResponse();
		try {
			secretsResource.addSecret(null, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
		}
		
		try {
			secretsResource.addSecret(new Secret(), asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
		}

		try {
			secretsResource.addSecret(new Secret().setKeyAs("{key}"), asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
		}
		
		try {
			secretsResource.addSecret(new Secret().setValueAs("value"), asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
		}

		try {
			secretsResource.addSecret(new Secret().setKeyAs("key")
					.setValueAs("value"), asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
		}
	}
	
	private AsyncResponse getOrCreateAsyncResponse() {
		if(asyncResponse == null)
			asyncResponse = MockingProxyBuilder.createMockingInvocationHandlerFor(AsyncResponse.class)
					.mock("resume(Object)").with(arguments->{
						if(arguments.get(0) instanceof List)
							secretKeys.put(arguments.get(0));
						if(arguments.get(0) instanceof Response)
							responses.put(arguments.get(0));
						return true;
					})
					.mock("toString").withEmptyMethod(()->"AsyncResponse").thenBuild();
		
		return asyncResponse;
	}

}
