package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.auth.server.totp.alternatives.AlternativeTOTPOptions;
import com.quakearts.auth.server.totp.generator.TOTPGenerator;
import com.quakearts.auth.server.totp.generator.impl.KeyGeneratorImpl;
import com.quakearts.auth.server.totp.generator.impl.TOTPGeneratorImpl;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.security.cryptography.jpa.EncryptedValue;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class TOTPGeneratorImplTest {

	@Inject
	private TOTPGeneratorImpl totpGenerator;
	
	@Inject
	private KeyGeneratorImpl keyGenerator;

	@Inject
	private TOTPOptions totpOptions;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testGenerateFor() throws Exception {
		Device device = new Device();
		device.setId("testgenerator1");
		keyGenerator.generateAndStoreIn(device);
		long time = System.currentTimeMillis()/totpOptions.getTimeStep();
		time *=totpOptions.getTimeStep();
		time += 2000;
		String[] testTokens = totpGenerator.generateFor(device, time);
		assertThat(testTokens.length, is(2));
		assertThat(testTokens[0], is(notNullValue()));
		assertThat(testTokens[1], is(nullValue()));
				
		time+=8500;
		String[] testTokens2 = totpGenerator.generateFor(device, time);
		assertThat(testTokens2.length, is(2));
		assertThat(testTokens2[0], is(notNullValue()));
		assertThat(testTokens2[1], is(notNullValue()));		
		
		AlternativeTOTPOptions.returnOtpLength(8);
		clearFormatField(totpGenerator);
		
		assertThat(totpOptions.getOtpLength(), is(8));		
		String[] testTokens3 = totpGenerator.generateFor(device, time);
		assertThat(testTokens3.length, is(2));
		assertThat(testTokens3[0], is(notNullValue()));
		assertThat(testTokens3[1], is(notNullValue()));
				
		expectedException.expect(ConfigurationException.class);
		AlternativeTOTPOptions.returnInvalidMacProvider();
		totpGenerator.generateFor(device, time);
	}
	
	@Test
	public void testRFC6238TestVectors() throws Exception {
		EncryptedValue seed = new EncryptedValue();
		seed.setValue("12345678901234567890123456789012".getBytes());

		Device device = new Device();
		device.setId("");
		device.setInitialCounter(0l);
		device.setSeed(seed);

		try {
			AlternativeTOTPOptions.returnTimeStep(30l);
			AlternativeTOTPOptions.returnOtpLength(8);
			
			assertThat(totpGenerator.generateFor(device, 59l)[0], is("46119246"));
			assertThat(totpGenerator.generateFor(device, 1111111109l)[0], is("68084774"));
			assertThat(totpGenerator.generateFor(device, 1111111111l)[0], is("67062674"));
			assertThat(totpGenerator.generateFor(device, 1234567890l)[0], is("91819424"));
			assertThat(totpGenerator.generateFor(device, 2000000000l)[0], is("90698825"));
			assertThat(totpGenerator.generateFor(device, 20000000000l)[0], is("77737706"));
			
			device.setId(UUID.randomUUID().toString());
			AlternativeTOTPOptions.returnInEnhancedMode(Boolean.FALSE);
			assertThat(totpGenerator.generateFor(device, 20000000000l)[0], is("77737706"));
		} finally {
			AlternativeTOTPOptions.returnTimeStep(null);
			AlternativeTOTPOptions.returnOtpLength(null);
			AlternativeTOTPOptions.returnInEnhancedMode(null);
			clearFormatField(totpGenerator);
		}
	}
	
	private void clearFormatField(TOTPGenerator generator) 
			throws NoSuchFieldException, SecurityException, 
			IllegalArgumentException, IllegalAccessException{
		Field formatField = TOTPGeneratorImpl.class
				.getDeclaredField("format");
		formatField.setAccessible(true);
		formatField.set(generator, null);
	}
}
