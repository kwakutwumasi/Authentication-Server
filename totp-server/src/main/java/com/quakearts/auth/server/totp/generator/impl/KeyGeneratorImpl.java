package com.quakearts.auth.server.totp.generator.impl;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.auth.server.totp.generator.KeyGenerator;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.security.cryptography.jpa.EncryptedValue;

@Singleton
public class KeyGeneratorImpl implements KeyGenerator {

	@Inject
	private TOTPOptions totpOptions;
	private SecureRandom secureRandom;
	
	@Override
	public void generateAndStoreIn(Device device) {
		byte[] generatedSeed = new byte[totpOptions.getSeedLength()];
		for(int index = 0; index<generatedSeed.length; index++) {
			generatedSeed[index] = (byte) getSecureRandom()
					.nextInt(255);
		}
		EncryptedValue encryptedValue = new EncryptedValue();
		encryptedValue.setValue(generatedSeed);
		encryptedValue.setDataStoreName(totpOptions.getDataStoreName());
		device.setSeed(encryptedValue);
	}

	private SecureRandom getSecureRandom() {
		if(secureRandom == null) {
			String instance = totpOptions.getSecureRandomInstance();
			String provider = totpOptions.getSecureRandomProvider();
			if(instance!=null && provider!=null) {
				try {
					secureRandom = SecureRandom.getInstance(instance,
							provider);
				} catch (GeneralSecurityException e) {
					throw new ConfigurationException(e);
				}
			} else {
				secureRandom = new SecureRandom();
			}
		}
		return secureRandom;
	}
}
