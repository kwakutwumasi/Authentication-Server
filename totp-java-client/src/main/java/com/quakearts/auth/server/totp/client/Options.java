package com.quakearts.auth.server.totp.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Options {
	private static final Options instance = new Options();
	
	private String macAlgorithm;
	private int otpLength;

	private long timeStep;

	private String totpUrl;

	private String totpWsUrl;

	private byte[] pbeSalt;

	private int pbeIterations;

	private long idleTimeout;
	
	public static Options getInstance() {
		return instance;
	}
	
	private Options() {
		Properties properties = new Properties();
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("options.properties");
		if(in!=null){
			try {
				properties.load(in);
			} catch (IOException e) {
				//Do nothing. won't happen
			}
		}
		
		macAlgorithm = properties.getProperty("macAlgorithm","HmacSHA256");
		otpLength = Integer.parseInt(properties.getProperty("otpLength","6"));
		timeStep = Long.parseLong(properties.getProperty("timeStep","30000"));
		totpUrl = properties.getProperty("totpUrl", "http://localhost:8082/totp-provisioning");
		totpWsUrl = properties.getProperty("totpWsUrl", "ws://localhost:8082/device-connection/{0}/{1}");
		pbeIterations = Integer.parseInt(properties.getProperty("pbeIterations", "23"));
		pbeSalt = properties.getProperty("pbeSalt","TOTP7079").getBytes();
		idleTimeout = Double.doubleToLongBits(
				Double.parseDouble(properties.getProperty("idleTimeout","30"))*60000d);
	}

	public String getMacAlgorithm() {
		return macAlgorithm;
	}

	public int getOtpLength() {
		return otpLength;
	}

	public long getTimeStep() {
		return timeStep;
	}
	
	public String getTotpUrl() {
		return totpUrl;
	}
	
	public String getTotpWsUrl() {
		return totpWsUrl;
	}

	public byte[] getPbeSalt() {
		return pbeSalt;
	}

	public int getPbeIterations() {
		return pbeIterations;
	}

	public long getIdleTimeout() {
		return idleTimeout;
	}
}
