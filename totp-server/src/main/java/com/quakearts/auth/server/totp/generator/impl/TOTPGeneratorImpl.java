package com.quakearts.auth.server.totp.generator.impl;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.auth.server.totp.generator.TOTPGenerator;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.security.cryptography.CryptoResource;

@Singleton
public class TOTPGeneratorImpl implements TOTPGenerator {

	@Inject
	private TOTPOptions totpOptions;
	private String format;
	private static final int[] POWER = {1,10,100,1000,10000,100000,1000000,10000000,100000000};
	
	@Override
	public String[] generateFor(Device device, long currentTimeInMillis) {
		String[] totps = new String[2];
		byte[] idBytes = device.getId().getBytes(StandardCharsets.UTF_8);
		try {
			long deltaInitCounter = currentTimeInMillis - device.getInitialCounter();
			long timeCounter = deltaInitCounter / totpOptions.getTimeStep();
			long lifespan = deltaInitCounter % totpOptions.getTimeStep();
			totps[0] = truncatedStringOf(generatedHmacFrom(
					timeValueUsing(timeCounter), idBytes,
					device.getSeed().getValue()));
			if (lifespan < totpOptions.getGracePeriod()) {
				timeCounter = (currentTimeInMillis - device.getInitialCounter() - totpOptions.getTimeStep()) / totpOptions.getTimeStep();
				totps[1] = truncatedStringOf(generatedHmacFrom(
						timeValueUsing(timeCounter), idBytes, 
						device.getSeed().getValue()));
			}
		} catch (GeneralSecurityException e) {
			throw new ConfigurationException("Unable to generate TOTP", e);
		}
		
		return totps;
	}

	private byte[] timeValueUsing(long timeCounter) {
		return ByteBuffer.allocate(8).putLong(timeCounter).array();
	}
	
	private byte[] generatedHmacFrom(byte[] currentTime, byte[] deviceIdBytes, byte[] seed) throws GeneralSecurityException {
		Mac mac = Mac.getInstance(totpOptions.getMacAlgorithm(), totpOptions.getMacProvider());
		SecretKey key = new SecretKeySpec(seed, totpOptions.getMacAlgorithm());
		mac.init(key);
		if(totpOptions.isInEnhancedMode())
			mac.update(deviceIdBytes);
		
		return mac.doFinal(currentTime);
	}
	
	private String truncatedStringOf(byte[] hashBytes) {
		int offset = hashBytes[hashBytes.length - 1] & 0xf;
		int code = (hashBytes[offset] & 0x7f) << 24 |
				(hashBytes[offset+1] & 0xff) << 16 |
				(hashBytes[offset+2] & 0xff) << 8 |
				hashBytes[offset+3] & 0xff;
		code = code % POWER[totpOptions.getOtpLength()];
		return String.format(getTemplate(), code);
	}

	private String getTemplate() {
		if(format == null)
			format = "%0"+totpOptions.getOtpLength()+"d";
		
		return format;
	}
	
	@Override
	public String signRequest(Device device, String request) {
		try {
			Mac mac = Mac.getInstance(totpOptions.getMacAlgorithm(), totpOptions.getMacProvider());
			byte[] idBytes = device.getId().getBytes(StandardCharsets.UTF_8);
			SecretKey key = new SecretKeySpec(device.getSeed().getValue(), totpOptions.getMacAlgorithm());
			mac.init(key);
			mac.update(idBytes);
			return CryptoResource.byteAsHex(mac.doFinal(request.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException e) {
			throw new ConfigurationException("Unable to generate TOTP", e);
		}
	}
}
