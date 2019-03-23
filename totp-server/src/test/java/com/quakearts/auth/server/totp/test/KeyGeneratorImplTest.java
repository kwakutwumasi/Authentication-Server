package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;

import java.lang.reflect.Field;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;


import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.auth.server.totp.alternatives.AlternativeTOTPOptions;
import com.quakearts.auth.server.totp.generator.KeyGenerator;
import com.quakearts.auth.server.totp.generator.impl.KeyGeneratorImpl;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class KeyGeneratorImplTest {
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Inject
	private KeyGeneratorImpl keyGenerator;

	@Inject
	private TOTPOptions totpOptions;
	
	@Test
	public void testGenerateAndStoreIn() throws Exception {
		clearSecureRandom(keyGenerator);
		Device device = new Device();
		keyGenerator.generateAndStoreIn(device);
		
		assertThat(device.getSeed(), is(notNullValue()));
		assertThat(device.getSeed().getValue(), is(notNullValue()));
		assertThat(device.getSeed().getValue().length, is(totpOptions.getSeedLength()));
		assertThat(device.getSeed().getDataStoreName(), is(notNullValue()));
		assertThat(device.getSeed().getDataStoreName(), is(totpOptions.getDataStoreName()));
				
		clearSecureRandom(keyGenerator);
		
		AlternativeTOTPOptions.returnNullSecureRandomGeneratorInstance();
		
		keyGenerator.generateAndStoreIn(device);

		assertThat(device.getSeed(), is(notNullValue()));
		assertThat(device.getSeed().getValue(), is(notNullValue()));
		assertThat(device.getSeed().getValue().length, is(totpOptions.getSeedLength()));
		assertThat(device.getSeed().getDataStoreName(), is(notNullValue()));
		assertThat(device.getSeed().getDataStoreName(), is(totpOptions.getDataStoreName()));
		

		clearSecureRandom(keyGenerator);
		
		AlternativeTOTPOptions.returnNullSecureRandomGeneratorProvider();
		
		keyGenerator.generateAndStoreIn(device);

		assertThat(device.getSeed(), is(notNullValue()));
		assertThat(device.getSeed().getValue(), is(notNullValue()));
		assertThat(device.getSeed().getValue().length, is(totpOptions.getSeedLength()));
		assertThat(device.getSeed().getDataStoreName(), is(notNullValue()));
		assertThat(device.getSeed().getDataStoreName(), is(totpOptions.getDataStoreName()));
	
		clearSecureRandom(keyGenerator);
	}
	
	@Test
	public void testGenerateAndStoreInWithException() throws Exception {
		clearSecureRandom(keyGenerator);			
		try{
			expectedException.expect(ConfigurationException.class);
			AlternativeTOTPOptions.returnInvalidSecureRandomGeneratorProvider();
			
			Device device = new Device();
			keyGenerator.generateAndStoreIn(device);			
		} finally {
			clearSecureRandom(keyGenerator);			
		}
	}

	private void clearSecureRandom(KeyGenerator instance) throws Exception {
		Field field = KeyGeneratorImpl.class.getDeclaredField("secureRandom");
		field.setAccessible(true);
		field.set(instance, null);
	}

}
