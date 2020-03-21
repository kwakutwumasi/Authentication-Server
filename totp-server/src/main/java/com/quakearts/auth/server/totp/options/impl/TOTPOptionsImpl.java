package com.quakearts.auth.server.totp.options.impl;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.appbase.internal.properties.ConfigurationPropertyMap;
import com.quakearts.auth.server.totp.options.TOTPConfigurationProvider;
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
	private int deviceConnectionPort;
	private int deviceConnectionThreads = 3;
	private int deviceConnectionReceiveBufferSize;
	private PerformancePreferences deviceConnectionPerformancePreferences;
	private Boolean deviceConnectionReuseAddress;
	private int deviceConnectionSocketTimeout;
	private String deviceConnectionSSLInstance;
	private String deviceConnectionKeystoreType;
	private String deviceConnectionKeystoreProvider;
	private String deviceConnectionKeystore;
	private String deviceConnectionKeystorePassword;
	private String deviceConnectionKeyPassword;
	private int executorServiceThreads = 3;
	private String serverJwtConfigName;
	private String allowedOrigins = "http://localhost:3000";
	
	@Inject
	private TOTPConfigurationProvider totpConfigurationProvider;
	private long deviceConnectionEchoInterval;
	private long deviceConnectionRequestTimeout;
	
	@SuppressWarnings("unchecked")
	@PostConstruct
	void init() {
		ConfigurationPropertyMap propertyMap = totpConfigurationProvider.getConfigurationPropertyMap(); 
		
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
		if(installedAdministrators == null) {
			throw new ConfigurationException("Entry 'installed.administrators' is missing from "
					+ totpConfigurationProvider.getConfigurationPropertyMapName()
					+" and is required");
		}
		if(propertyMap.containsKey("allowed.origins")){			
			allowedOrigins = propertyMap.getString("allowed.origins");
		}		
		if(propertyMap.containsKey("server.jwt.config")){
			serverJwtConfigName = propertyMap.getString("server.jwt.config");
		} else {
			serverJwtConfigName = "login.config";
		}
		countQuery = propertyMap.getString("count.query");
		
		ConfigurationPropertyMap deviceConnectionMap;
		deviceConnectionMap = propertyMap.getSubConfigurationPropertyMap("device.connection");
		
		if(deviceConnectionMap != null) {
			if(deviceConnectionMap.containsKey("threads")) {
				deviceConnectionThreads = deviceConnectionMap.getInt("threads");
			}
			deviceConnectionPort = deviceConnectionMap.getInt("port");
			deviceConnectionKeystorePassword = deviceConnectionMap.getString("keystore.password");
			deviceConnectionKeystoreType = deviceConnectionMap.getString("keystore.type");
			deviceConnectionKeystoreProvider = deviceConnectionMap.getString("keystore.provider");
			deviceConnectionKeystore = deviceConnectionMap.getString("keystore");
			deviceConnectionReceiveBufferSize = deviceConnectionMap.getInt("receive.buffer.size");
			if(deviceConnectionMap.containsKey("reuse.address")) {
				deviceConnectionReuseAddress = deviceConnectionMap
						.getBoolean("reuse.address");
			}
			deviceConnectionSocketTimeout = deviceConnectionMap.getInt("socket.timeout");
			deviceConnectionSSLInstance = deviceConnectionMap.getString("ssl.instance");
			
			if(deviceConnectionMap.containsKey("performance.preferences")) {
				ConfigurationPropertyMap performancePreferencesMap = deviceConnectionMap
							.getSubConfigurationPropertyMap("performance.preferences");
				deviceConnectionPerformancePreferences = new PerformancePreferences(
						performancePreferencesMap.getInt("connection.time"), 
						performancePreferencesMap.getInt("latency"), 
						performancePreferencesMap.getInt("bandwidth"));
			}
			deviceConnectionEchoInterval = deviceConnectionMap.getLong("echo.interval");
			if(deviceConnectionMap.containsKey("executor.service.threads")) {
				executorServiceThreads = deviceConnectionMap.getInt("executor.service.threads");
			}
			
			deviceConnectionRequestTimeout = deviceConnectionMap.getLong("request.timeout");
		} else {
			throw new ConfigurationException("Entry 'device.connection' is missing from "
					+ totpConfigurationProvider.getConfigurationPropertyMapName()
					+" and is required");
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

	@Override
	public int getDeviceConnectionPort() {
		return deviceConnectionPort;
	}

	@Override
	public int getDeviceConnectionThreads() {
		return deviceConnectionThreads;
	}

	@Override
	public int getDeviceConnectionReceiveBufferSize() {
		return deviceConnectionReceiveBufferSize;
	}

	@Override
	public PerformancePreferences getDeviceConnectionPerformancePreferences() {
		return deviceConnectionPerformancePreferences;
	}

	@Override
	public Boolean getDeviceConnectionReuseAddress() {
		return deviceConnectionReuseAddress;
	}

	@Override
	public int getDeviceConnectionSocketTimeout() {
		return deviceConnectionSocketTimeout;
	}

	@Override
	public String getDeviceConnectionSSLInstance() {
		return deviceConnectionSSLInstance;
	}

	@Override
	public String getDeviceConnectionKeystoreType() {
		return deviceConnectionKeystoreType;
	}

	@Override
	public String getDeviceConnectionKeystoreProvider() {
		return deviceConnectionKeystoreProvider;
	}

	@Override
	public String getDeviceConnectionKeystore() {
		return deviceConnectionKeystore;
	}

	@Override
	public String getDeviceConnectionKeystorePassword() {
		return deviceConnectionKeystorePassword;
	}

	@Override
	public String getDeviceConnectionKeyPassword() {
		return deviceConnectionKeyPassword;
	}
	
	@Override
	public long getDeviceConnectionEchoInterval() {
		return deviceConnectionEchoInterval;
	}
	
	@Override
	public int getExecutorServiceThreads() {
		return executorServiceThreads;
	}
	
	@Override
	public long getDeviceConnectionRequestTimeout() {
		return deviceConnectionRequestTimeout;
	}
	
	@Override
	public String getServerJwtConfigName() {
		return serverJwtConfigName;
	}
	
	@Override
	public String getAllowedOrigins() {
		return allowedOrigins;
	}
}
