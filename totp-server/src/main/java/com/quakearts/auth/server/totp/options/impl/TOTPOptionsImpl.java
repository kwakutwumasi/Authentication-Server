package com.quakearts.auth.server.totp.options.impl;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.appbase.internal.properties.ConfigurationPropertyMap;
import com.quakearts.auth.server.totp.options.TOTPConfigurationProvider;
import com.quakearts.auth.server.totp.options.TOTPOptions;

@Singleton
public class TOTPOptionsImpl implements TOTPOptions {
	
	private static final Logger log = LoggerFactory.getLogger(TOTPOptions.class);
	
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
	private String requestSigningJwtConfigName;
	private boolean inEnhancedMode = true;
	
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
		
		if(propertyMap.containsKey("request.signing.jwt.config")){
			requestSigningJwtConfigName = propertyMap.getString("request.signing.jwt.config");
		} else {
			requestSigningJwtConfigName = "login.config";
		}
		
		countQuery = propertyMap.getString("count.query");
		
		if(propertyMap.containsKey("in.enhanced.mode"))
			inEnhancedMode = propertyMap.getBoolean("in.enhanced.mode");
		
		getDeviceConnectionParameters(propertyMap);
	}

	private void getDeviceConnectionParameters(ConfigurationPropertyMap propertyMap) {
		ConfigurationPropertyMap deviceConnectionMap;
		deviceConnectionMap = propertyMap.getSubConfigurationPropertyMap("device.connection");
		
		if(deviceConnectionMap != null) {
			if(deviceConnectionMap.containsKey("threads")) {
				deviceConnectionThreads = deviceConnectionMap.getInt("threads");
			}
			deviceConnectionPort = deviceConnectionMap.getInt("port");
			deviceConnectionKeystorePassword = deviceConnectionMap.getString("keystore.password");
			if(deviceConnectionMap.containsKey("key.password"))
				deviceConnectionKeyPassword = deviceConnectionMap.getString("key.password");
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
		log.debug("dataStoreName: {}", dataStoreName);
		return dataStoreName;
	}
	
	@Override
	public String getMacAlgorithm() {
		log.debug("macAlgorithm: {}", macAlgorithm);
		return macAlgorithm;
	}

	@Override
	public String getMacProvider() {
		log.debug("macProvider: {}", macProvider);
		return macProvider;
	}

	@Override
	public int getOtpLength() {
		log.debug("otpLength: {}", otpLength);
		return otpLength;
	}
	
	@Override
	public int getSeedLength() {
		log.debug("seedLength: {}", seedLength);
		return seedLength;
	}
	
	@Override
	public String getSecureRandomInstance() {
		log.debug("secureRandomInstance: {}", secureRandomInstance);
		return secureRandomInstance;
	}
	
	@Override
	public String getSecureRandomProvider() {
		log.debug("secureRandomProvider: {}", secureRandomProvider);
		return secureRandomProvider;
	}
	
	@Override
	public long getTimeStep() {
		log.debug("timeStep: {}", timeStep);
		return timeStep;
	}
	
	@Override
	public long getGracePeriod() {
		log.debug("gracePeriod: {}", gracePeriod);
		return gracePeriod;
	}
	
	@Override
	public int getMaxAttempts() {
		log.debug("maxAttempts: {}", maxAttempts);
		return maxAttempts;
	}
	
	@Override
	public int getLockoutTime() {
		log.debug("lockoutTime: {}", lockoutTime);
		return lockoutTime;
	}
	
	@Override
	public Map<String, String> getInstalledAdministrators() {
		log.debug("installedAdministrators: {}", installedAdministrators!=null? installedAdministrators.size(): "0");
		return installedAdministrators;
	}
	
	@Override
	public String getCountQuery() {
		log.debug("countQuery: {}", countQuery);
		return countQuery;
	}

	@Override
	public int getDeviceConnectionPort() {
		log.debug("deviceConnectionPort: {}", deviceConnectionPort);
		return deviceConnectionPort;
	}

	@Override
	public int getDeviceConnectionThreads() {
		log.debug("deviceConnectionThreads: {}", deviceConnectionThreads);
		return deviceConnectionThreads;
	}

	@Override
	public int getDeviceConnectionReceiveBufferSize() {
		log.debug("deviceConnectionReceiveBufferSize: {}", deviceConnectionReceiveBufferSize);
		return deviceConnectionReceiveBufferSize;
	}

	@Override
	public PerformancePreferences getDeviceConnectionPerformancePreferences() {
		log.debug("deviceConnectionPerformancePreferences: {}", deviceConnectionPerformancePreferences);
		return deviceConnectionPerformancePreferences;
	}

	@Override
	public Boolean getDeviceConnectionReuseAddress() {
		log.debug("deviceConnectionReuseAddress: {}", deviceConnectionReuseAddress);
		return deviceConnectionReuseAddress;
	}

	@Override
	public int getDeviceConnectionSocketTimeout() {
		log.debug("deviceConnectionSocketTimeout: {}", deviceConnectionSocketTimeout);
		return deviceConnectionSocketTimeout;
	}

	@Override
	public String getDeviceConnectionSSLInstance() {
		log.debug("deviceConnectionSSLInstance: {}", deviceConnectionSSLInstance);
		return deviceConnectionSSLInstance;
	}

	@Override
	public String getDeviceConnectionKeystoreType() {
		log.debug("deviceConnectionKeystoreType: {}", deviceConnectionKeystoreType);
		return deviceConnectionKeystoreType;
	}

	@Override
	public String getDeviceConnectionKeystoreProvider() {
		log.debug("deviceConnectionKeystoreProvider: {}", deviceConnectionKeystoreProvider);
		return deviceConnectionKeystoreProvider;
	}

	@Override
	public String getDeviceConnectionKeystore() {
		log.debug("deviceConnectionKeystore: {}", deviceConnectionKeystore);
		return deviceConnectionKeystore;
	}

	@Override
	public String getDeviceConnectionKeystorePassword() {
		log.debug("deviceConnectionKeystorePassword: {}", deviceConnectionKeyPassword!=null? "**********":"[is null]");
		return deviceConnectionKeystorePassword;
	}

	@Override
	public String getDeviceConnectionKeyPassword() {
		log.debug("deviceConnectionKeyPassword: {}", deviceConnectionKeyPassword!=null? "**********":"[is null]");
		return deviceConnectionKeyPassword;
	}
	
	@Override
	public long getDeviceConnectionEchoInterval() {
		log.debug("deviceConnectionEchoInterval: {}", deviceConnectionEchoInterval);
		return deviceConnectionEchoInterval;
	}
	
	@Override
	public int getExecutorServiceThreads() {
		log.debug("executorServiceThreads: {}", executorServiceThreads);
		return executorServiceThreads;
	}
	
	@Override
	public long getDeviceConnectionRequestTimeout() {
		log.debug("deviceConnectionRequestTimeout: {}", deviceConnectionRequestTimeout);
		return deviceConnectionRequestTimeout;
	}
	
	@Override
	public String getServerJwtConfigName() {
		log.debug("serverJwtConfigName: {}", serverJwtConfigName);
		return serverJwtConfigName;
	}
	
	@Override
	public String getAllowedOrigins() {
		log.debug("allowedOrigins: {}", allowedOrigins);
		return allowedOrigins;
	}
	
	@Override
	public String getRequestSigningJwtConfigName() {
		log.debug("requestSigningJwtConfigName: {}", requestSigningJwtConfigName);
		return requestSigningJwtConfigName;
	}
	
	@Override
	public boolean isInEnhancedMode() {
		log.debug("inEnhancedMode: {}", inEnhancedMode);
		return inEnhancedMode;
	}
}
