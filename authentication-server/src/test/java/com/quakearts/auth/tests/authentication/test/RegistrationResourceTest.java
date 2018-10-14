package com.quakearts.auth.tests.authentication.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNot.*;
import static org.hamcrest.core.IsNull.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import org.infinispan.Cache;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.rest.RegistrationResource;
import com.quakearts.auth.server.rest.models.ErrorResponse;
import com.quakearts.auth.server.rest.models.LoginConfiguration;
import com.quakearts.auth.server.rest.models.LoginConfigurationEntry;
import com.quakearts.auth.server.rest.models.Registration;
import com.quakearts.auth.server.store.annotation.AliasStore;
import com.quakearts.auth.server.store.annotation.RegistryStore;
import com.quakearts.auth.server.store.annotation.SecretsStore;
import com.quakearts.auth.server.rest.models.LoginConfigurationEntry.ModuleFlag;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;

@RunWith(MainRunner.class)
public class RegistrationResourceTest {
	private final ArrayBlockingQueue<Registration> registrations = new ArrayBlockingQueue<>(1);
	private final ArrayBlockingQueue<Response> responses = new ArrayBlockingQueue<>(1);
	private final ArrayBlockingQueue<ErrorResponse> errorResponses = new ArrayBlockingQueue<>(1);
	private AsyncResponse asyncResponse;
	
	@Rule
	public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
		
	@Inject @RegistryStore
	private Cache<String, Registration> store;
	@Inject @AliasStore
	private Cache<String, String> aliases;
	@Inject @SecretsStore
	private Cache<String, String> secrets;

	@Before
	public void addSecrets() {
		secrets.put("{secret.key}", "SecretValue");
	}

	@After
	public void clearStore() {
		store.clear();
		aliases.clear();
		secrets.clear();
	}
	
	@Test
	public void testRegisterGetByIdUpdateRegistrationUnregister() throws Exception {
		registrations.clear();
		AsyncResponse asyncResponse = getOrCreateAsyncResponse();
		
		Registration registration = new Registration()
				.setIdAs("test-register-1")
				.setAliasAs("test-register-alias-1")
				.createConfiguration()
					.setNameAs("Test-Config")
						.createEntry()
							.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
							.setModuleFlagAs(ModuleFlag.REQUIRED)
						.thenAdd()
				.thenAdd();
		
		registrationResource.register(registration, asyncResponse);
		assertThat(responses.take().getStatus(), is(204));
		registrationResource.getById("test-register-1", asyncResponse);
		Registration gottenRegistration = registrations.take();
		assertThat(gottenRegistration, is(registration));
		assertThat(aliases.get("test-register-alias-1"), is("test-register-1"));
		assertThat(registration.getOptions().get("algorithm"), is("HS256"));
		assertThat(registration.getOptions().get("secret"), is(notNullValue()));
		assertThat(registration.getOptions().get("issuer"), is("https://quakearts.com"));
		assertThat(registration.getOptions().get("audience"), is("https://quakearts.com"));
		assertThat(registration.getOptions().get("validity.period"), is("15 Minutes"));
		assertThat(registration.getOptions().get("grace.period"), is("1"));

		errorResponses.clear();
		
		registrationResource.register(registration, asyncResponse);
		ErrorResponse actualErrorResponse = errorResponses.take();
		assertThat(actualErrorResponse.getCode(), is("existing-id"));
		assertThat(actualErrorResponse.getExplanations().contains("A registration with the provided ID/alias already exists"), 
				is(true));
		
		Registration anotherRegistration = new Registration()
				.setIdAs("test-another")
				.setAliasAs("test-register-alias-1")
				.createConfiguration()
					.setNameAs("Test-Config")
						.createEntry()
							.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
							.setModuleFlagAs(ModuleFlag.REQUIRED)
						.thenAdd()
				.thenAdd();
		
		registrationResource.register(anotherRegistration, asyncResponse);
		actualErrorResponse = errorResponses.take();
		assertThat(actualErrorResponse.getCode(), is("existing-id"));
		assertThat(actualErrorResponse.getExplanations().contains("A registration with the provided ID/alias already exists"), 
				is(true));

		anotherRegistration = new Registration()
				.setIdAs("test-another")
				.setAliasAs("test-another-alias")
				.createConfiguration()
					.setNameAs("Test-Config")
						.createEntry()
							.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
							.setModuleFlagAs(ModuleFlag.REQUIRED)
						.thenAdd()
				.thenAdd()
				.addOption("secret", "{secret.key}")
				.addOption("audience", "{secret.value}")
				.addOption("rolesgroupname", "{secret.value")
				.addOption("password", "secret.value}");
		
		registrationResource.register(anotherRegistration, asyncResponse);
		assertThat(responses.take().getStatus(), is(204));
		
		assertThat(anotherRegistration.getOptions().get("secret"), is("SecretValue"));
		assertThat(anotherRegistration.getOptions().get("rolesgroupname"), is("{secret.value"));
		assertThat(anotherRegistration.getOptions().get("password"), is("secret.value}"));
		assertThat(anotherRegistration.getOptions().get("audience"), is("{secret.value}"));
		assertThat(anotherRegistration.getOptions().get("algorithm"), is("HS256"));
		assertThat(anotherRegistration.getOptions().get("issuer"), is("https://quakearts.com"));
		assertThat(anotherRegistration.getOptions().get("validity.period"), is("15 Minutes"));
		assertThat(anotherRegistration.getOptions().get("grace.period"), is("1"));
		
		Registration updateRegistration = new Registration()
				.setAliasAs("test-register-alias-1")
				.createConfiguration()
				.setNameAs("Test-Config")
					.createEntry()
						.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
						.setModuleFlagAs(ModuleFlag.REQUIRED)
						.addOption("value", "true")
					.thenAdd()
					.createEntry()
						.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
						.setModuleFlagAs(ModuleFlag.REQUIRED)
					.addOption("value", "false")
				.thenAdd()
			.thenAdd()
			.addOption("secret", "secret");
		
		registrationResource.updateRegistration("test-register-1", updateRegistration, asyncResponse);
		assertThat(responses.take().getStatus(), is(204));
		registrationResource.getById("test-register-1", asyncResponse);
		gottenRegistration = registrations.take();
		assertThat(registration, is(not(updateRegistration)));
		assertThat(gottenRegistration, is(updateRegistration));
		
		updateRegistration = new Registration()
					.setAliasAs("test-register-changed-alias")
					.createConfiguration()
					.setNameAs("Test-Config")
						.createEntry()
							.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
							.setModuleFlagAs(ModuleFlag.REQUIRED)
							.addOption("value", "true")
						.thenAdd()
						.createEntry()
							.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
							.setModuleFlagAs(ModuleFlag.REQUIRED)
						.addOption("value", "false")
					.thenAdd()
				.thenAdd()
				.addOption("secret", "secret");
		
		registrationResource.updateRegistration("test-register-1", updateRegistration, asyncResponse);
		assertThat(responses.take().getStatus(), is(204));
		assertThat(aliases.get("test-register-changed-alias"), is("test-register-1"));
		assertThat(aliases.containsKey("test-register-alias-1"), is(false));
		
		updateRegistration
			.addOption("password", "{secret.key}")
			.addOption("audience", "{secret.value}")
			.addOption("rolesgroupname", "{secret.value")
			.addOption("secret", "secret.value}");
		registrationResource.updateRegistration("test-register-1", updateRegistration, asyncResponse);
		assertThat(responses.take().getStatus(), is(204));
		assertThat(updateRegistration.getOptions().get("password"), is("SecretValue"));
		assertThat(updateRegistration.getOptions().get("rolesgroupname"), is("{secret.value"));
		assertThat(updateRegistration.getOptions().get("secret"), is("secret.value}"));
		assertThat(updateRegistration.getOptions().get("audience"), is("{secret.value}"));

		
		registrationResource.unregister("test-register-1", asyncResponse);
		assertThat(responses.take().getStatus(), is(204));
		registrationResource.getById("test-register-1", asyncResponse);
		actualErrorResponse = errorResponses.take();
		assertThat(actualErrorResponse.getCode(), is("invalid-id"));
		assertThat(actualErrorResponse.getExplanations()
				.contains("A registration with the provided ID could not be found"), is(true));
		assertThat(aliases.containsKey("test-register-changed-alias"), is(false));
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
						if(arguments.get(0) instanceof Registration)
							registrations.put(arguments.get(0));
						if(arguments.get(0) instanceof Response)
							responses.put(arguments.get(0));
						return true;
					})
					.mock("toString").withEmptyMethod(()->"AsyncResponse").thenBuild();
		
		return asyncResponse;
	}
	
	@Test
	public void testGetByIdNotFound() throws Exception {
		errorResponses.clear();
		AsyncResponse response = getOrCreateAsyncResponse();
		
		registrationResource.getById("test-not-found", response);
		ErrorResponse actualErrorResponse = errorResponses.take();
		assertThat(actualErrorResponse.getCode(), is("invalid-id"));
		assertThat(actualErrorResponse.getExplanations()
				.contains("A registration with the provided ID could not be found"), is(true));
	}

	@Test
	public void testValidUpdatedRegistration() throws Exception {
		AsyncResponse asyncResponse = getOrCreateAsyncResponse();
		
		Registration registration = new Registration()
				.setIdAs("test-register-2")
				.setAliasAs("test-register-2-alias")
				.createConfiguration()
					.setNameAs("Test-Config")
						.createEntry()
							.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
							.setModuleFlagAs(ModuleFlag.REQUIRED)
						.thenAdd()
				.thenAdd();
		
		store.put("test-register-2", registration);
		aliases.put("test-register-2-alias", "test-register-2");
		
		try {
			registrationResource.updateRegistration("test-register-2", null, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Invalid Registration"));
			violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration' cannot be null"));
		}
		
		registration = new Registration();
		
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration.alias' cannot be null"));
		}
		
		registration.setAlias("    ");

		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration.alias' cannot be null"));
		}
		
		registration.setAlias("test-register-2-alias");
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("You must specify at least one item in 'configurations'"));
		}
		
		registration.getConfigurations().add(new LoginConfiguration());
		
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration.loginConfiguration.name' is required"));
		}

		registration.getConfigurations().get(0)
			.setName("   ");
		
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {	
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration.loginConfiguration.name' is required"));
		}
		
		registration.getConfigurations().get(0)
			.setName("Test");
	
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {		
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("You must specify at least one item in 'entries'"));
		}
		
		registration.getConfigurations().get(0)
			.getEntries().add(new LoginConfigurationEntry());
	
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("Parameter 'registration.loginConfiguration.entry.moduleClassname' is required"));
		}
		
		registration.getConfigurations().get(0)
			.getEntries().get(0).setModuleClassname("  ");
	
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("Parameter 'registration.loginConfiguration.entry.moduleClassname' is required"));
		}
		
		registration.getConfigurations().get(0)
			.getEntries().get(0).setModuleClassname("TestLoginModule");
	
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {		
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("Parameter 'registration.loginConfiguration.entry.moduleFlag' is required"));
		}
		
		registration.getConfigurations().get(0)
			.getEntries().get(0).setModuleFlag(ModuleFlag.REQUIRED);
		registration.getOptions().put("invalid", "property");

		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {		
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("Option key 'invalid' is invalid"));
		}
		
		registration
			.addOption("validity.period", "")
			.getOptions().remove("invalid");

		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.validity.period' is invalid"));
		}
		
		registration
			.getOptions().put("validity.period", "1");
		
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.validity.period' is invalid"));
		}

		registration
			.getOptions().put("validity.period", "   ");
		
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.validity.period' is invalid"));
		}
		
		registration
		.getOptions().put("validity.period", "1  ");
	
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.validity.period' is invalid"));
		}
		
		registration
			.getOptions().put("validity.period", "  Days");
	
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {	
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.validity.period' is invalid"));
		}
		
		registration.addOption("validity.period", "1 Days").addOption("activate.after.period", "");
		
		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("The 'registration.options.activate.after.period' is invalid"));
		}
		
		registration.getOptions().put("activate.after.period", "  ");

		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("The 'registration.options.activate.after.period' is invalid"));
		}

		registration.getOptions().put("activate.after.period", "  Minute");

		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("The 'registration.options.activate.after.period' is invalid"));
		}
		
		registration.getOptions().put("activate.after.period", "1");

		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("The 'registration.options.activate.after.period' is invalid"));
		}

		registration.addOption("activate.after.period", "1 Minute")
			.addOption("algorithm", "");

		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {	
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.algorithm' is invalid"));
		}
		
		registration.getOptions().put("algorithm", "HSA");

		try {
			registrationResource.updateRegistration("test-register-2", registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {	
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.algorithm' is invalid"));
		}

		registration.getOptions().put("algorithm", "HS256");
		registrationResource.updateRegistration("test-register-2", registration, asyncResponse);
		Response response = responses.take();
		assertThat(response.getStatus(), is(204));

		registration.getOptions().put("algorithm", "ES256");
		registrationResource.updateRegistration("test-register-2", registration, asyncResponse);
		response = responses.take();
		assertThat(response.getStatus(), is(204));

		registration.getOptions().put("algorithm", "RS256");
		registrationResource.updateRegistration("test-register-2", registration, asyncResponse);
		response = responses.take();
		assertThat(response.getStatus(), is(204));

	}
	
	private List<ConstraintViolation<?>> sort(Set<ConstraintViolation<?>> violations){
		List<ConstraintViolation<?>> list = new ArrayList<>(violations);
		Collections.sort(list, (violation1, violation2)-> violation1.getMessage().equals("Invalid Registration")?-1:1);
		return list;
	}
	
	@Test
	public void testValidNewRegistration() throws Exception {
		AsyncResponse asyncResponse = getOrCreateAsyncResponse();
		try {
			registrationResource.register(null, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Invalid Registration"));
			violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration' cannot be null"));
		}
		
		Registration registration = new Registration();
		
		try {
			registrationResource.register(registration, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration.id' cannot be null"));
		}
		
		registration.setId(" ");
		
		try {
			registrationResource.register(registration, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration.id' cannot be null"));
		}	
		
		registration.setIdAs("test-register-3");
		
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration.alias' cannot be null"));
		}
		
		registration.setAlias("    ");

		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration.alias' cannot be null"));
		}
		
		registration.setAlias("test-register-3-alias");
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("You must specify at least one item in 'configurations'"));
		}
		
		registration.getConfigurations().add(new LoginConfiguration());
		
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration.loginConfiguration.name' is required"));
		}

		registration.getConfigurations().get(0)
			.setName("   ");
		
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {	
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("Parameter 'registration.loginConfiguration.name' is required"));
		}
		
		registration.getConfigurations().get(0).setName("Test");
	
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {		
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("You must specify at least one item in 'entries'"));
		}
		
		registration.getConfigurations().get(0)
			.getEntries().add(new LoginConfigurationEntry());
	
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("Parameter 'registration.loginConfiguration.entry.moduleClassname' is required"));
		}
		
		registration.getConfigurations().get(0)
			.getEntries().get(0).setModuleClassname("  ");
	
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("Parameter 'registration.loginConfiguration.entry.moduleClassname' is required"));
		}
		
		registration.getConfigurations().get(0)
			.getEntries().get(0).setModuleClassname("TestLoginModule");
	
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {		
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("Parameter 'registration.loginConfiguration.entry.moduleFlag' is required"));
		}
		
		registration.getConfigurations().get(0)
			.getEntries().get(0).setModuleFlag(ModuleFlag.REQUIRED);
		registration.getOptions().put("invalid", "property");

		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {		
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("Option key 'invalid' is invalid"));
		}
		
		registration.addOption("validity.period", "")
			.getOptions().remove("invalid");

		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.validity.period' is invalid"));
		}
		
		registration
			.getOptions().put("validity.period", "1");
		
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.validity.period' is invalid"));
		}

		registration
			.getOptions().put("validity.period", "   ");
		
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.validity.period' is invalid"));
		}
		
		registration
		.getOptions().put("validity.period", "1  ");
	
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.validity.period' is invalid"));
		}
		
		registration
			.getOptions().put("validity.period", "  Days");
	
		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {	
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.validity.period' is invalid"));
		}
		
		registration.addOption("validity.period", "1 Day")
			.addOption("activate.after.period", "");
		
		try {
			registrationResource.register(registration, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("The 'registration.options.activate.after.period' is invalid"));
		}
		
		registration.getOptions().put("activate.after.period", "  ");

		try {
			registrationResource.register(registration, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("The 'registration.options.activate.after.period' is invalid"));
		}

		registration.getOptions().put("activate.after.period", "  Minute");

		try {
			registrationResource.register(registration, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("The 'registration.options.activate.after.period' is invalid"));
		}
		
		registration.getOptions().put("activate.after.period", "1");

		try {
			registrationResource.register(registration, asyncResponse);
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), is("The 'registration.options.activate.after.period' is invalid"));
		}

		registration.addOption("activate.after.period", "1 Minute")
			.addOption("algorithm", "");

		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {	
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.algorithm' is invalid"));
		}
		
		registration.getOptions().put("algorithm", "HSA");

		try {
			registrationResource.register(registration, asyncResponse);			
			fail("Violation was not thrown");
		} catch (ConstraintViolationException e) {	
			assertThat(e.getConstraintViolations().size(), is(2));
			Iterator<ConstraintViolation<?>> iterator = sort(e.getConstraintViolations()).iterator();
			iterator.next();
			ConstraintViolation<?> violation = iterator.next();
			assertThat(violation.getMessage(), 
					is("The 'registration.options.algorithm' is invalid"));
		}
		
		registration.getOptions().put("algorithm", "HS256");
				
		registrationResource.register(registration, asyncResponse);
		Response response = responses.take();
		assertThat(response.getStatus(), is(204));
		
		registration = new Registration()
				.setIdAs("test-register-4")
				.setAliasAs("test-register-4-alias")
				.createConfiguration()
					.setNameAs("Test-Config")
						.createEntry()
							.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
							.setModuleFlagAs(ModuleFlag.REQUIRED)
						.thenAdd()
				.thenAdd()
				.addOption("algorithm", "ES256");
		
		registrationResource.register(registration, asyncResponse);
		response = responses.take();
		assertThat(response.getStatus(), is(204));
		
		registration = new Registration()
				.setIdAs("test-register-5")
				.setAliasAs("test-register-5-alias")
				.createConfiguration()
					.setNameAs("Test-Config")
						.createEntry()
							.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
							.setModuleFlagAs(ModuleFlag.REQUIRED)
						.thenAdd()
				.thenAdd()
				.addOption("algorithm", "ES256");

		registration.getOptions().put("algorithm", "RS256");
		registrationResource.register(registration, asyncResponse);
		response = responses.take();
		assertThat(response.getStatus(), is(204));

	}
			
	@Test
	public void testUpdateRegistrationWithNonExistentId() throws Exception {
		errorResponses.clear();
		AsyncResponse response = getOrCreateAsyncResponse();

		Registration updateRegistration = new Registration()
				.setAliasAs("test7alias")
				.createConfiguration()
				.setNameAs("Test-Config")
					.createEntry()
						.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
						.setModuleFlagAs(ModuleFlag.REQUIRED)
					.thenAdd()
			.thenAdd();
		
		registrationResource.updateRegistration("test-non-existent",
				updateRegistration, response);
		ErrorResponse actualErrorResponse = errorResponses.take();
		assertThat(actualErrorResponse.getCode(), is("invalid-id"));
		assertThat(actualErrorResponse.getExplanations()
				.contains("A registration with the provided ID could not be found"), is(true));
	}
	
	@Test
	public void testUpdateRegistrationWithExistingAlias() throws Exception {
		errorResponses.clear();
		AsyncResponse response = getOrCreateAsyncResponse();

		Registration updateRegistration = new Registration()
				.setIdAs("test-already-existing")
				.setAliasAs("test5Alias")
				.createConfiguration()
				.setNameAs("Test-Config")
					.createEntry()
						.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
						.setModuleFlagAs(ModuleFlag.REQUIRED)
					.thenAdd()
			.thenAdd();
		store.put("test-already-existing", updateRegistration);
		aliases.put("test5Alias", "test-already-existing");
		aliases.put("test6Alias", "test-another-existing");
		
		updateRegistration = new Registration()
				.setAliasAs("test6Alias")
				.createConfiguration()
				.setNameAs("Test-Config")
					.createEntry()
						.setModuleClassnameAs("com.quakearts.auth.server.test.TestLoginModule")
						.setModuleFlagAs(ModuleFlag.REQUIRED)
					.thenAdd()
			.thenAdd();
		
		registrationResource.updateRegistration("test-already-existing",
				updateRegistration, response);
		ErrorResponse actualErrorResponse = errorResponses.take();
		assertThat(actualErrorResponse.getCode(), is("existing-id"));
		assertThat(actualErrorResponse.getExplanations()
				.contains("A registration with the provided ID/alias already exists"), is(true));
	}
	
	@Test
	public void testUnregisterWithNonExistentId() throws Exception {
		errorResponses.clear();
		AsyncResponse response = getOrCreateAsyncResponse();
		registrationResource.unregister("test-non-existent", response);
		ErrorResponse actualErrorResponse = errorResponses.take();
		assertThat(actualErrorResponse.getCode(), is("invalid-id"));
		assertThat(actualErrorResponse.getExplanations()
				.contains("A registration with the provided ID could not be found"), is(true));
	}

	@Test
	public void testRegistrationMarshalling() throws Exception {
		Registration registration = new ObjectMapper()
				.readValue("{\"options\":null}", Registration.class);
		
		assertThat(registration.getOptions(), is(notNullValue()));
	}
	
	@Inject
	private RegistrationResource registrationResource;	
}
