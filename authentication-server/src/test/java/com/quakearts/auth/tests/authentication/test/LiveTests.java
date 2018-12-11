package com.quakearts.auth.tests.authentication.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import org.infinispan.Cache;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.rest.models.Secret;
import com.quakearts.auth.server.rest.models.LoginConfigurationEntry.ModuleFlag;
import com.quakearts.auth.server.store.annotation.AliasStore;
import com.quakearts.auth.server.store.annotation.RegistryStore;
import com.quakearts.auth.server.store.annotation.SecretsStore;
import com.quakearts.auth.tests.authentication.loginmodule.TestLoginModule;
import com.quakearts.auth.tests.authentication.test.client.Registration;
import com.quakearts.auth.tests.authentication.test.client.TestClient;
import com.quakearts.auth.tests.authentication.test.client.TestClientBuilder;
import com.quakearts.auth.tests.authentication.test.client.TokenResponse;
import com.quakearts.auth.tests.authentication.test.client.TestClient.AuthenticationRequest;
import com.quakearts.auth.tests.authentication.test.client.TestClient.TestClientException;
import com.quakearts.webapp.security.jwt.JWTVerifier;
import com.quakearts.webapp.security.jwt.factory.JWTFactory;

@RunWith(MainRunner.class)
public class LiveTests {

	@Rule
	public ExpectedException exception = ExpectedException.none();
	private TestClient client;
	
	@Inject @RegistryStore
	private Cache<String, com.quakearts.auth.server.rest.models.Registration> store;
	@Inject @AliasStore
	private Cache<String, String> aliases;
	@Inject @SecretsStore
	private Cache<String, String> secrets;
	
	@Before
	public void createTestClient() {
		store.clear();
		aliases.clear();
		secrets.clear();

		client = TestClientBuilder.createNewTestClient()
				.setURLAs("http://localhost:8180").thenBuild();
	}
	
	@Test
	public void testAll() throws Exception {
		client.addSecretValue(new Secret().withKeyAs("{secret}")
				.withValueAs("1234567890"));

		assertThat(client.getHttpResponse().getHeader("Access-Control-Allow-Origin"),
				is("*"));
		assertThat(client.getHttpResponse().getHeader("Access-Control-Allow-Methods"),
				is("*"));
		assertThat(client.getHttpResponse().getHeader("Access-Control-Allow-Headers"),
				is("*"));
		
		assertThat(secrets.size(), is(1));
		assertThat(secrets.get("{secret}"), is("1234567890"));
		
		client.addSecretValue(new Secret().withKeyAs("{secret.two}")
				.withValueAs("0987654321"));
		
		assertThat(secrets.size(), is(2));
		assertThat(secrets.get("{secret}"), is("1234567890"));
		assertThat(secrets.get("{secret.two}"), is("0987654321"));
		
		client.removeSecretValue("{secret.two}");
		
		assertThat(secrets.size(), is(1));
		assertThat(secrets.get("{secret}"), is("1234567890"));
		assertThat(secrets.get("{secret.two}"), is(nullValue()));
		
		client.register(new Registration()
				.setIdAs("test-rest")
				.setAliasAs("test-rest")
				.addOption("secret", "{secret}")
				.createConfiguration()
					.setNameAs("Test")
					.createEntry()
						.setModuleClassnameAs(TestLoginModule.class.getName())
						.setModuleFlagAs(ModuleFlag.REQUIRED)
						.addOption("test", "value")
					.thenAdd()
				.thenAdd());
		
		com.quakearts.auth.server.rest.models.Registration registration = store.get("test-rest");
		assertThat(registration.getAlias(), is("test-rest"));
		assertThat(registration.getId(), is("test-rest"));
		assertThat(registration.getConfigurations().get(0).getName(), is("Test"));
		assertThat(registration.getConfigurations().get(0)
				.getEntries().get(0).getModuleClassname(), is(TestLoginModule.class.getName()));
		assertThat(registration.getConfigurations()
				.get(0).getEntries().get(0).getModuleFlag(), is(ModuleFlag.REQUIRED));
		assertThat(registration.getConfigurations()
				.get(0).getEntries().get(0).getOptions().get("test"), is("value"));
		assertThat(registration.getOptions().get("validity.period"), is("15 Minutes"));
		assertThat(registration.getOptions().get("secret"), is("1234567890"));
		
		List<String> aliasesList = client.getAllAliases();
		assertThat(aliasesList.isEmpty(), is(false));
		assertThat(aliasesList.size(), is(1));
		assertThat(aliasesList.contains("test-rest"), is(true));
		
		client.update("test-rest", new Registration()
						.setAliasAs("test-rest")
						.addOption("secret", "W@h8237HksIhfmsd2Nl94WNCA")
						.createConfiguration()
							.setNameAs("Test")
							.createEntry()
								.setModuleClassnameAs(TestLoginModule.class.getName())
								.setModuleFlagAs(ModuleFlag.REQUIRED)
								.addOption("test", "value")
							.thenAdd()
						.thenAdd());
		
		registration = store.get("test-rest");
		assertThat(registration.getOptions().get("secret"), is("W@h8237HksIhfmsd2Nl94WNCA"));

		TokenResponse response = client.authenticate(new AuthenticationRequest()
				.setAliasAs("test-rest")
				.setApplicationAs("Test")
				.setClientAs("test")
				.setCredentialAs("dGVzdDE="));
		
		assertThat(response.getExpiresIn()>0, is(true));
		assertThat(response.getIdToken(), is(notNullValue()));
		assertThat(response.getTokenType(), is(notNullValue()));
		
		JWTVerifier verifier = 
				JWTFactory.getInstance().getVerifier("HS256", registration.getOptions());
		
		verifier.verify(response.getIdToken().getBytes());
		
		client.delete("test-rest");
		
		assertThat(store.get("test-rest"), is(nullValue()));
		
		Map<String, Object> configuration = new HashMap<>();
		configuration.put("jndi.name","MyLiveDs");
		configuration.put("driverClassName","org.apache.derby.jdbc.EmbeddedDriver");
		configuration.put("url","jdbc:derby:MyLiveDs");
		configuration.put("defaultAutoCommit",true);
		configuration.put("initialSize", 5);
		configuration.put("maxActive", 10);
		configuration.put("minIdle", 1);
		configuration.put("connectionProperties","create=true;");

		client.createDataSource("testcreatedatasource", configuration);
		assertThat(client.listDataSources().contains("java:/jdbc/MyLiveDs"), is(true));		
		client.removeDataSource("testcreatedatasource");
		assertThat(client.listDataSources().contains("java:/jdbc/MyLiveDs"), is(false));		
	}
	
	@Test
	public void testValidationMapper() throws Exception {
		try {
			client.register(new Registration()
				.setIdAs("test-rest"));
		} catch (TestClientException e) {
			assertThat(e.getErrorResponse().getCode(), is("invalid-data"));
			assertThat(e.getErrorResponse().getExplanations()
					.contains("Invalid Registration"), is(true));
			assertThat(e.getErrorResponse().getExplanations()
					.contains("Parameter 'registration.alias' cannot be null"), is(true));
		}
	}

}
