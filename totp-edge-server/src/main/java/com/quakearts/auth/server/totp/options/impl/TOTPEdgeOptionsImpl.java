package com.quakearts.auth.server.totp.options.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Singleton;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.auth.server.totp.options.TOTPEdgeOptions;

@Singleton
public class TOTPEdgeOptionsImpl implements TOTPEdgeOptions {
	
	private String totpUrl;
	private String totpServerIp;
	private int totpServerPort;
	private String keystoreType;
	private String keystore;
	private String keystorePassword;
	private String keyPassword;
	private String sslInstance;
	private String jwtalgorithm;
	private Map<String, Object> jwtOptions;
	private long payloadQueueTimeout;
	private int payloadQueueSize;
	
	public TOTPEdgeOptionsImpl() {
		Properties properties = new Properties();
		try(InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("totp.config.properties")){
			if(in!=null)
				properties.load(in);
		} catch (IOException e) {
			throw new ConfigurationException(e);
		}
		
		totpUrl = properties.getProperty("totp.url", "http://localhost:8081/totp");
		totpServerIp = properties.getProperty("totp.server.ip", "localhost");
		totpServerPort = Integer.parseInt(properties.getProperty("totp.server.port", "9001"));
		keystoreType = properties.getProperty("keystore.type","PKCS12");
		keystore = properties.getProperty("keystore", "totp.keystore");
		keystorePassword = properties.getProperty("keystore.password", "password");
		keyPassword = properties.getProperty("key.password", keystorePassword);
		sslInstance = properties.getProperty("ssl.instance","TLSv1.2");
		jwtalgorithm = properties.getProperty("jwt.algorithm", "HS256");
		jwtOptions = new HashMap<>();
		properties.entrySet()
			.stream().forEach(entry->{
				if(entry.getKey().toString().startsWith("jwt.")) {
					jwtOptions.put(entry.getKey().toString().substring(4), entry.getValue());
				}
			});
		payloadQueueSize = Integer.parseInt(properties.getProperty("payload.queue.size", "10"));
		payloadQueueTimeout = Long.parseLong(properties.getProperty("payload.queue.timeout", "30000"));
	}

	@Override
	public String getTotpUrl() {
		return totpUrl;
	}

	@Override
	public String getTotpServerIp() {
		return totpServerIp;
	}

	@Override
	public int getTotpServerPort() {
		return totpServerPort;
	}

	@Override
	public String getKeystoreType() {
		return keystoreType;
	}

	@Override
	public String getKeystore() {
		return keystore;
	}

	@Override
	public String getKeystorePassword() {
		return keystorePassword;
	}

	@Override
	public String getKeyPassword() {
		return keyPassword;
	}
	
	@Override
	public String getSslInstance() {
		return sslInstance;
	}
	
	@Override
	public String getJwtalgorithm() {
		return jwtalgorithm;
	}
	
	@Override
	public Map<String, ?> getJwtOptions() {
		return jwtOptions;
	}
	
	@Override
	public int getPayloadQueueSize() {
		return payloadQueueSize;
	}
	
	@Override
	public long getPayloadQueueTimeout() {
		return payloadQueueTimeout;
	}
}
