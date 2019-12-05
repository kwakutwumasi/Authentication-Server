package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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

	private String format;
	
	@Test
	public void testGenerateFor() throws Exception {
		format = "%0"+totpOptions.getOtpLength()+"d";
		Device device = new Device();
		device.setId("testgenerator1");
		keyGenerator.generateAndStoreIn(device);
		long time = System.currentTimeMillis()/totpOptions.getTimeStep();
		time *=totpOptions.getTimeStep();
		time += 2000;
		String[] testTokens = totpGenerator.generateFor(device, time);
		assertThat(testTokens.length, is(2));
		assertThat(testTokens[0], is(generateOTP(time, device.getId(), device.getSeed().getValue(), device.getInitialCounter())));
		assertThat(testTokens[1], is(nullValue()));
				
		time+=3000;
		String[] testTokens2 = totpGenerator.generateFor(device, time);
		assertThat(testTokens2.length, is(2));
		assertThat(testTokens2[0], is(generateOTP(time, device.getId(), device.getSeed().getValue(), device.getInitialCounter())));
		assertThat(testTokens2[1], is(generateOTP(time-3000, device.getId(), device.getSeed().getValue(), device.getInitialCounter())));		
		
		AlternativeTOTPOptions.returnOtpLength(8);
		clearFormatField(totpGenerator);
		
		assertThat(totpOptions.getOtpLength(), is(8));
		format = "%0"+totpOptions.getOtpLength()+"d";
		
		String[] testTokens3 = totpGenerator.generateFor(device, time);
		assertThat(testTokens3.length, is(2));
		assertThat(testTokens3[0], is(generateOTP(time, device.getId(), device.getSeed().getValue(), device.getInitialCounter())));
		assertThat(testTokens3[1], is(generateOTP(time-3000, device.getId(), device.getSeed().getValue(), device.getInitialCounter())));
				
		expectedException.expect(ConfigurationException.class);
		AlternativeTOTPOptions.returnInvalidMacProvider();
		totpGenerator.generateFor(device, time);
	}
	
	private void clearFormatField(TOTPGenerator generator) 
			throws NoSuchFieldException, SecurityException, 
			IllegalArgumentException, IllegalAccessException{
		Field formatField = TOTPGeneratorImpl.class
				.getDeclaredField("format");
		formatField.setAccessible(true);
		formatField.set(generator, null);
	}
	
	private String generateOTP(long totpTimestamp, String id, byte[] seed, long initialValue) throws GeneralSecurityException {
		return truncatedStringOf(generatedHmacFrom(
				timeValueUsing(totpTimestamp, initialValue),id, seed));
	}
	
	private String truncatedStringOf(byte[] hashBytes) {
		int offset = Math.abs(hashBytes[hashBytes.length-1] 
				% (hashBytes.length-4));
		int code = (hashBytes[offset] & 0x7f) << 24 |
				(hashBytes[offset+1] & 0xff) << 16 |
				(hashBytes[offset+2] & 0xff) << 8 |
				hashBytes[offset+3] & 0xff;
		code = (int) (code % Math.pow(10, totpOptions.getOtpLength()));
		return String.format(format, code);
	}

	private byte[] generatedHmacFrom(byte[] currentTime, String id, byte[] seed) throws GeneralSecurityException {
		Mac mac = Mac.getInstance(totpOptions.getMacAlgorithm());
		SecretKey key = new SecretKeySpec(seed, totpOptions.getMacAlgorithm());
		mac.init(key);
		mac.update(id.getBytes());
		return mac.doFinal(currentTime);
	}

	private byte[] timeValueUsing(long currentTimeInMillis, long initialCounter) {
		long timestamp = (currentTimeInMillis - initialCounter) / totpOptions.getTimeStep();
		return ByteBuffer.allocate(8).putLong(timestamp).array();
	}


}
