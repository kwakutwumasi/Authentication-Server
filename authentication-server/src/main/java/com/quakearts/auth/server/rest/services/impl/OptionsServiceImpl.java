package com.quakearts.auth.server.rest.services.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.infinispan.Cache;

import java.security.SecureRandom;

import com.quakearts.auth.server.rest.services.OptionsService;
import com.quakearts.auth.server.store.annotation.SecretsStore;
import com.quakearts.webapp.security.jwt.signature.HMac;

@Singleton
public class OptionsServiceImpl implements OptionsService {
	private static final char[][] charSet = {
			{0x30,0x39},//Digits 0-9
			{0x41,0x5a},//A-Z
			{0x61,0x7a}//a-z
	};
	
	private static final char[][] symbolSet = {
			{0x21,0x2f},//Symbol set 1
			{0x3a,0x40},//Symbol set 2
			{0x5b,0x60},//Symbol set 3
			{0x7b,0x7d}//Symbol set 4
	};
	
	@Inject @SecretsStore
	private Cache<String, String> secretsStore;
	
	private final Map<String, String> defaultOptions = getDefault();

	private Map<String, String> getDefault() {
		Map<String, String> defaultOptions = new HashMap<>();
		defaultOptions.put("algorithm", HMac.HSAlgorithmType.HS256.toString());
		defaultOptions.put("secret", generateRandom());
		defaultOptions.put("issuer", "https://quakearts.com");
		defaultOptions.put("audience", "https://quakearts.com");
		defaultOptions.put("validity.period", "15 Minutes");
		defaultOptions.put("grace.period", "1");
		return Collections.unmodifiableMap(defaultOptions);
	}
	
	private String generateRandom() {
		StringBuilder randomString = new StringBuilder();
		SecureRandom random = new SecureRandom();
		while (randomString.length()<32) {
			int charOrSymbol = random.nextInt(4);			
			switch (charOrSymbol) {
			case 3:
				int set = random.nextInt(symbolSet.length);
				generateRandomChar(randomString, random, symbolSet[set]);
				break;
			default:
				generateRandomChar(randomString, random, charSet[charOrSymbol]);
				break;
			}
		}
		return randomString.substring(0, 32);
	}

	private void generateRandomChar(StringBuilder randomString, 
			SecureRandom random, char[] array) {
		char randomChar = (char) (random.nextInt(array[1]-array[0])+array[0]);
		randomString.append(randomChar);
	}

	/* (non-Javadoc)
	 * @see com.quakearts.auth.server.rest.services.OptionsService#buildOptions(java.util.Map)
	 */
	@Override
	public Map<String, String> buildOptions(Map<String, String> customOptions) {
		resolveSecrets(customOptions);
		Map<String, String> options = new HashMap<>(defaultOptions);
		options.putAll(customOptions);
		return options;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void resolveSecrets(Map<String, ? extends Object> parameters){
		parameters.entrySet().forEach(entry->{
			if(entry.getValue() instanceof String && entry.getValue()
						.toString().startsWith("{")
					&& entry.getValue()
						.toString().endsWith("}")
					&& secretsStore.containsKey(entry.getValue().toString())) {
				Entry<String, String> stringEntry = (Entry<String, String>) entry;
				stringEntry.setValue(secretsStore.get(entry.getValue()));
			}
		});
	}

}
