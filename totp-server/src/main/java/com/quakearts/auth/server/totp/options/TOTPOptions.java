package com.quakearts.auth.server.totp.options;

import java.util.Map;

public interface TOTPOptions {

	public class PerformancePreferences {

		private int connectionTime;
        private int latency;
        private int bandwidth;
        
		public PerformancePreferences(int connectionTime, int latency, int bandwidth) {
			this.connectionTime = connectionTime;
			this.latency = latency;
			this.bandwidth = bandwidth;
		}

		public int getConnectionTime() {
			return connectionTime;
		}

		public int getLatency() {
			return latency;
		}

		public int getBandwidth() {
			return bandwidth;
		}

	}

	String getDataStoreName();

	String getMacAlgorithm();

	String getMacProvider();

	int getOtpLength();

	int getSeedLength();

	String getSecureRandomInstance();

	String getSecureRandomProvider();

	long getTimeStep();

	long getGracePeriod();

	int getMaxAttempts();

	int getLockoutTime();

	Map<String, String> getInstalledAdministrators();
	
	String getCountQuery();
	
	int getDeviceConnectionPort();

	int getDeviceConnectionThreads();

	int getDeviceConnectionReceiveBufferSize();

	PerformancePreferences getDeviceConnectionPerformancePreferences();

	Boolean getDeviceConnectionReuseAddress();

	int getDeviceConnectionSocketTimeout();

	String getDeviceConnectionSSLInstance();

	String getDeviceConnectionKeystoreType();

	String getDeviceConnectionKeystoreProvider();

	String getDeviceConnectionKeystore();

	String getDeviceConnectionKeystorePassword();

	String getDeviceConnectionKeyPassword();
	
	long getDeviceConnectionEchoInterval();
	
	int getExecutorServiceThreads();
	
	long getDeviceConnectionRequestTimeout();

	String getServerJwtConfigName();

	String getAllowedOrigins();

	String getRequestSigningJwtConfigName();
}