package com.quakearts.auth.server.totp.generator.impl;

import java.nio.ByteBuffer;
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

@Singleton
public class TOTPGeneratorImpl implements TOTPGenerator {

	@Inject
	private TOTPOptions totpOptions;
	private String format;
	
	@Override
	public String[] generateFor(Device device, long currentTimeInMillis) {
		String[] totps = new String[2];
		byte[] idBytes = device.getId().getBytes();
		try {
			totps[0] = truncatedStringOf(generatedHmacFrom(
					timeValueUsing(device.getInitialCounter(), totpOptions.getTimeStep(), currentTimeInMillis),
					device.getSeed().getValue(), idBytes));
			if (currentTimeInMillis % totpOptions.getTimeStep() < totpOptions.getGracePeriod()) {
				totps[1] = truncatedStringOf(generatedHmacFrom(
						timeValueUsing(device.getInitialCounter(), totpOptions.getTimeStep(), currentTimeInMillis - totpOptions.getTimeStep()), 
						device.getSeed().getValue(), idBytes));
			}
		} catch (GeneralSecurityException e) {
			throw new ConfigurationException("Unable to generate TOTP", e);
		}
		
		return totps;
	}

	private byte[] timeValueUsing(long initialCounter, long timeStep, long currentTimeInMillis) {
		long timestamp = (currentTimeInMillis - initialCounter) / timeStep;
		return ByteBuffer.allocate(8).putLong(timestamp).array();
	}
	
	private byte[] generatedHmacFrom(byte[] currentTime, byte[] deviceIdBytes, byte[] seed) throws GeneralSecurityException {
		Mac mac = Mac.getInstance(totpOptions.getMacAlgorithm(), totpOptions.getMacProvider());
		SecretKey key = new SecretKeySpec(seed, totpOptions.getMacAlgorithm());
		mac.init(key);
		mac.update(deviceIdBytes);
		return mac.doFinal(currentTime);
	}
	
	private String truncatedStringOf(byte[] hashBytes) {
		int offset = Math.abs(hashBytes[hashBytes.length-1] 
				% (hashBytes.length-4));
		int code = (hashBytes[offset] & 0x7f) << 24 |
				(hashBytes[offset+1] & 0xff) << 16 |
				(hashBytes[offset+2] & 0xff) << 8 |
				hashBytes[offset+3] & 0xff;
		code = (int) (code % Math.pow(10, totpOptions.getOtpLength()));
		return String.format(getTemplate(), code);
	}

	private String getTemplate() {
		if(format == null)
			format = "%0"+totpOptions.getOtpLength()+"d";
		
		return format;
	}
}
