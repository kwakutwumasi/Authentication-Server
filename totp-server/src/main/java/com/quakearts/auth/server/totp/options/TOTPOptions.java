package com.quakearts.auth.server.totp.options;

import java.util.Map;

public interface TOTPOptions {

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
	
	enum GlobalDefaults {;
		public static final String LOGIN_MODULE = "TOTP-JWT-Login";
	}
}