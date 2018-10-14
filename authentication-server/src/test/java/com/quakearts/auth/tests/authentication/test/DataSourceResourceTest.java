package com.quakearts.auth.tests.authentication.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import org.infinispan.Cache;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.rest.DataSourcesResource;
import com.quakearts.auth.server.rest.models.ErrorResponse;
import com.quakearts.auth.server.rest.services.InitialContextService;
import com.quakearts.auth.server.store.annotation.SecretsStore;
import com.quakearts.auth.tests.authentication.test.cdi.AlternativesProducer;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;

@RunWith(MainRunner.class)
public class DataSourceResourceTest {
	
	@Rule
	public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

	@Inject
	private DataSourcesResource resource;
	@Inject
	private InitialContextService contextService;
	@Inject @SecretsStore
	private Cache<String, String> secretStore;
	
	private final ArrayBlockingQueue<Response> responses = new ArrayBlockingQueue<>(1);
	private final ArrayBlockingQueue<ErrorResponse> errorResponses = new ArrayBlockingQueue<>(1);
	private AsyncResponse asyncResponse;

	@Test
	public void testAll() throws Exception {
		AsyncResponse asyncResponse = getOrCreateAsyncResponse();
		
		secretStore.put("{connection.properties.secret}", "create=true;");
		Map<String, String> configuration = newMap()
				.add("jndi.name","MyDs")
				.add("driverClassName","org.apache.derby.jdbc.EmbeddedDriver")
				.add("url","jdbc:derby:MyDs")
				.add("connectionProperties","{connection.properties.secret}").thenBuld();
		
		resource.addDatasource("testResourceKey1", configuration, asyncResponse);
		assertThat(responses.take().getStatus(), is(204));
		
		assertThat(configuration.get("connectionProperties"), is("create=true;"));
		
		assertThat(contextService.lookup("java:/jdbc/MyDs"), is(notNullValue()));
		assertThat(resource.listAllDataSources().contains("java:/jdbc/MyDs"), is(true));
		
		resource.removeDatasource("testResourceKey1", asyncResponse);
		assertThat(responses.take().getStatus(), is(204));
		assertThat(resource.listAllDataSources().contains("java:/jdbc/MyDs"), is(false));
		assertThat(new File("etc"+File.separator+"testResourceKey1.ds.json").exists(), is(false));
	}
	
	@Test
	public void testFileSaveErrors() throws Exception {
		AsyncResponse asyncResponse = getOrCreateAsyncResponse();
		resource.addDatasource("testDSFileExistsError", newMap()
				.add("jndi.name", "TestDS1")
				.thenBuld(), asyncResponse);
		ErrorResponse errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("The datasource key already exists"), is(true));

		resource.addDatasource("testDSFileSaveError", newMap()
				.add("jndi.name", "TestDS2")
				.thenBuld(), asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Cannot save file"), is(true));
	}
	
	@Test
	public void testDSCreationErrors() throws Exception {
		AsyncResponse asyncResponse = getOrCreateAsyncResponse();
		resource.addDatasource("testDSConfigError", newMap()
				.add("jndi.name", "TestDS3")
				.thenBuld(), asyncResponse);
		ErrorResponse errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Error configuring datasource"), is(true));
		assertThat(AlternativesProducer.getLastDeletedFiles().getName(), is("testDSConfigError.ds.json"));

		resource.addDatasource("testDSConfigErrorAndDeleteError", newMap()
				.add("jndi.name", "TestDS4")
				.thenBuld(), asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Error configuring datasource"), is(true));
		assertThat(errorResponse.getExplanations().contains("Cannot delete file"), is(true));
	}
	
	@Test
	public void testDSTestErrors() throws Exception {
		AsyncResponse asyncResponse = getOrCreateAsyncResponse();
		resource.addDatasource("testDSTestError", newMap()
				.add("jndi.name", "TestDSLookupError")
				.thenBuld(), asyncResponse);
		ErrorResponse errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Unable to lookup Name"), is(true));
		assertThat(AlternativesProducer.getLastDeletedFiles().getName(), is("testDSTestError.ds.json"));

		resource.addDatasource("testDSTestErrorAndDeleteError", newMap()
				.add("jndi.name", "TestDSLookupError")
				.thenBuld(), asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Unable to lookup Name"), is(true));
		assertThat(errorResponse.getExplanations().contains("Cannot delete file"), is(true));
		
		resource.addDatasource("testDSTestError", newMap()
				.add("jndi.name", "TestDSGetConnectionError")
				.thenBuld(), asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Cannot connect"), is(true));
		assertThat(AlternativesProducer.getLastRemoved().getName(), is("testDSTestError.ds.json"));
		assertThat(AlternativesProducer.getLastDeletedFiles().getName(), is("testDSTestError.ds.json"));
		
		resource.addDatasource("testDSTestErrorAndRemoveError", newMap()
				.add("jndi.name", "TestDSGetConnectionError")
				.thenBuld(), asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Cannot connect"), is(true));
		assertThat(errorResponse.getExplanations().contains("Error removing datasource"), is(true));
		assertThat(AlternativesProducer.getLastDeletedFiles().getName(), is("testDSTestErrorAndRemoveError.ds.json"));

		resource.addDatasource("testDSTestErrorAndDeleteError", newMap()
				.add("jndi.name", "TestDSGetConnectionError")
				.thenBuld(), asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Cannot connect"), is(true));
		assertThat(errorResponse.getExplanations().contains("Cannot delete file"), is(true));
		assertThat(AlternativesProducer.getLastRemoved().getName(), is("testDSTestErrorAndDeleteError.ds.json"));

		resource.addDatasource("testDSTestErrorAndRemoveErrorAndDeleteError", newMap()
				.add("jndi.name", "TestDSGetConnectionError")
				.thenBuld(), asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Cannot connect"), is(true));
		assertThat(errorResponse.getExplanations().contains("Error removing datasource"), is(true));
		assertThat(errorResponse.getExplanations().contains("Cannot delete file"), is(true));

		resource.addDatasource("testDSTestError", newMap()
				.add("jndi.name", "TestDSGetCatalogError")
				.thenBuld(), asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Cannot connect to catalog"), is(true));
		assertThat(AlternativesProducer.getLastRemoved().getName(), is("testDSTestError.ds.json"));
		assertThat(AlternativesProducer.getLastDeletedFiles().getName(), is("testDSTestError.ds.json"));

		resource.addDatasource("testDSTestErrorAndRemoveError", newMap()
				.add("jndi.name", "TestDSGetCatalogError")
				.thenBuld(), asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Cannot connect to catalog"), is(true));
		assertThat(errorResponse.getExplanations().contains("Error removing datasource"), is(true));
		assertThat(AlternativesProducer.getLastDeletedFiles().getName(), is("testDSTestErrorAndRemoveError.ds.json"));

		resource.addDatasource("testDSTestErrorAndDeleteError", newMap()
				.add("jndi.name", "TestDSGetCatalogError")
				.thenBuld(), asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Cannot connect to catalog"), is(true));
		assertThat(errorResponse.getExplanations().contains("Cannot delete file"), is(true));
		assertThat(AlternativesProducer.getLastRemoved().getName(), is("testDSTestErrorAndDeleteError.ds.json"));

		resource.addDatasource("testDSTestErrorAndRemoveErrorAndDeleteError", newMap()
				.add("jndi.name", "TestDSGetCatalogError")
				.thenBuld(), asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations().contains("Cannot connect to catalog"), is(true));
		assertThat(errorResponse.getExplanations().contains("Error removing datasource"), is(true));
		assertThat(errorResponse.getExplanations().contains("Cannot delete file"), is(true));
	}
	
	@Test
	public void testRemoveErrors() throws Exception {
		AsyncResponse asyncResponse = getOrCreateAsyncResponse();
		resource.removeDatasource("testDSFileNotExistsError", asyncResponse);
		ErrorResponse errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations()
				.contains("The data source file could not be found"),is(true));
		
		resource.removeDatasource("testDSFileExistsAndRemoveError", asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations()
				.contains("Error removing datasource"),is(true));

		resource.removeDatasource("testDSFileExistsAndDeleteError", asyncResponse);
		errorResponse = errorResponses.take();
		assertThat(errorResponse.getCode(), is("datasource-error"));
		assertThat(errorResponse.getExplanations()
				.contains("Cannot delete file"),is(true));
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
						responses.put(arguments.get(0));
						return true;
					})
					.thenBuild();
		
		return asyncResponse;
	}
	
	private MapBuilder newMap() {
		return new MapBuilder();
	}

	class MapBuilder {
		Map<String, String> map = new HashMap<>();
			
		MapBuilder add(String key, String value) {
			map.put(key, value);
			return this;
		}
		
		Map<String, String> thenBuld(){
			return map;
		}
	}
}
