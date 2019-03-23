package com.quakearts.auth.server.totp.options.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.inject.Singleton;

import com.quakearts.appbase.Main;
import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.appbase.internal.properties.ConfigurationPropertyMap;
import com.quakearts.auth.server.totp.options.TOTPOptions;

@Singleton
public class TOTPOptionsImpl implements TOTPOptions {
	private String dataStoreName;
	private String macAlgorithm;
	private String macProvider;
	private int otpLength;
	private int seedLength;
	private String secureRandomInstance;
	private String secureRandomProvider;
	private long timeStep;
	private long gracePeriod;
	private int maxAttempts;
	private int lockoutTime;
	private Map<String, String> installedAdministrators;
	private String countQuery;
	
	@SuppressWarnings("unchecked")
	public TOTPOptionsImpl() {
		try {
			InputStream inputStream = Thread.currentThread()
					.getContextClassLoader().getResourceAsStream("totpoptions.json");
			ConfigurationPropertyMap propertyMap = Main.getAppBasePropertiesLoader()
					.loadParametersFromReader("totpoptions.json", new InputStreamReader(inputStream));
			dataStoreName = propertyMap.getString("data.store.name");
			macAlgorithm = propertyMap.getString("mac.algorithm");
			macProvider = propertyMap.getString("mac.provider");
			otpLength = propertyMap.getInt("otp.length");
			seedLength = propertyMap.getInt("seed.length");
			secureRandomInstance = propertyMap.getString("secure.random.instance");
			secureRandomProvider = propertyMap.getString("secure.random.provider");
			timeStep = propertyMap.getInt("time.step")*1000l;
			gracePeriod = propertyMap.getInt("grace.period")*1000l;
			maxAttempts = propertyMap.getInt("max.attempts");
			lockoutTime = propertyMap.getInt("lockout.time");
			installedAdministrators = propertyMap.get("installed.administrators", Map.class);
			countQuery = propertyMap.getString("count.query");
		} catch (IOException | NullPointerException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public String getDataStoreName() {
		return dataStoreName;
	}
	
	@Override
	public String getMacAlgorithm() {
		return macAlgorithm;
	}

	@Override
	public String getMacProvider() {
		return macProvider;
	}

	@Override
	public int getOtpLength() {
		return otpLength;
	}
	
	@Override
	public int getSeedLength() {
		return seedLength;
	}
	
	@Override
	public String getSecureRandomInstance() {
		return secureRandomInstance;
	}
	
	@Override
	public String getSecureRandomProvider() {
		return secureRandomProvider;
	}
	
	@Override
	public long getTimeStep() {
		return timeStep;
	}
	
	@Override
	public long getGracePeriod() {
		return gracePeriod;
	}
	
	@Override
	public int getMaxAttempts() {
		return maxAttempts;
	}
	
	@Override
	public int getLockoutTime() {
		return lockoutTime;
	}
	
	@Override
	public Map<String, String> getInstalledAdministrators() {
		return installedAdministrators;
	}
	
	@Override
	public String getCountQuery() {
		return countQuery;
	}
}
