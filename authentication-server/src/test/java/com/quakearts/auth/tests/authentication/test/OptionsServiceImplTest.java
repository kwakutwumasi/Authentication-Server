package com.quakearts.auth.tests.authentication.test;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.infinispan.Cache;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.rest.services.impl.OptionsServiceImpl;
import com.quakearts.auth.server.store.annotation.SecretsStore;

@RunWith(MainRunner.class)
public class OptionsServiceImplTest {

	@Inject
	private OptionsServiceImpl serviceImpl;
	
	@Inject @SecretsStore
	private Cache<String, String> secretsStore;
	
	@Test
	public void testBuildOptions() {
		Map<String, String> options = serviceImpl.buildOptions(new HashMap<>());
		
		assertThat(options.get("algorithm"), is("HS256"));
		assertThat(options.get("secret"), is(notNullValue()));
		assertThat(options.get("issuer"), is("https://quakearts.com"));
		assertThat(options.get("audience"), is("https://quakearts.com"));
		assertThat(options.get("validity.period"), is("15 Minutes"));
		assertThat(options.get("grace.period"), is("1"));
		
		secretsStore.put("{top.secret}", "123456789");
		Map<String, String> testMap = new HashMap<>();
		testMap.put("audience", "https://demo.quakearts.com");
		testMap.put("secret", "{top.secret}");
		testMap.put("password", "{top.secret.2}");
		
		options = serviceImpl.buildOptions(testMap);
		
		assertThat(options.get("algorithm"), is("HS256"));
		assertThat(options.get("secret"), is("123456789"));
		assertThat(options.get("issuer"), is("https://quakearts.com"));
		assertThat(options.get("audience"), is("https://demo.quakearts.com"));
		assertThat(options.get("validity.period"), is("15 Minutes"));
		assertThat(options.get("grace.period"), is("1"));
		assertThat(options.get("password"), is("{top.secret.2}"));
	}

	@Test
	public void testResolveSecrets() {
		secretsStore.put("{secret.password1}", "123456789");
		Map<String, Object> testMap = new HashMap<>();
		testMap.put("password", "{secret.password2}");
		testMap.put("database.password","{secret.password1}");
		testMap.put("count", 1);
		testMap.put("resolve", Boolean.TRUE);
		
		serviceImpl.resolveSecrets(testMap);
		assertThat(testMap.get("password"), is("{secret.password2}"));
		assertThat(testMap.get("database.password"), is("123456789"));
		assertThat(testMap.get("count"), is(1));
		assertThat(testMap.get("resolve"), is(Boolean.TRUE));
	}

}
