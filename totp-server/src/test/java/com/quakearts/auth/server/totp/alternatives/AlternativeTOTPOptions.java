package com.quakearts.auth.server.totp.alternatives;

import java.util.Map;
import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.auth.server.totp.options.impl.TOTPOptionsImpl;

@Alternative
@Singleton
@Priority(Interceptor.Priority.APPLICATION)
public class AlternativeTOTPOptions implements TOTPOptions {
	
	private static boolean returnNullSecureRandomGeneratorInstance;
	
	public static void returnNullSecureRandomGeneratorInstance() {
		returnNullSecureRandomGeneratorInstance = true;
	}
	
	private static boolean returnNullSecureRandomGeneratorProvider;
	
	public static void returnNullSecureRandomGeneratorProvider() {
		returnNullSecureRandomGeneratorProvider = true;
	}
	
	private static boolean returnInvalidSecureRandomGeneratorProvider;
	
	public static void returnInvalidSecureRandomGeneratorProvider() {
		returnInvalidSecureRandomGeneratorProvider = true;
	}
	
	private static boolean returnInvalidMacProvider;
	
	public static void returnInvalidMacProvider() {
		returnInvalidMacProvider = true;
	}
	
	private static Integer returnOtpLength;
	
	public static void returnOtpLength(Integer newLength){
		returnOtpLength = newLength;
	}
	
	private static String returnCountQuery;
	
	public static void returnCountQuery(String newCountQuery){
		returnCountQuery = newCountQuery;
	}
	
	private static Long returnConnectionEchoInterval;
	
	public static void returnConnectionEchoInterval(Long newConnectionEchoInterval) {
		returnConnectionEchoInterval = newConnectionEchoInterval;
	}
	
	private static Long returnDeviceAuthenticationTimeout;
	
	public static void returnDeviceAuthenticationTimeout(Long newDeviceAuthenticationTimeout) {
		returnDeviceAuthenticationTimeout = newDeviceAuthenticationTimeout;
	}
	
	@Inject
	private TOTPOptionsImpl wrapped;
	
	private static Integer returnConnectionReceiveBufferSize;
	
	public static void returnConnectionReceiveBufferSize(Integer newConnectionReceiveBufferSize) {
		returnConnectionReceiveBufferSize = newConnectionReceiveBufferSize;
	}
	
	private static PerformancePreferences returnPerformancePreferences;
	
	public static void returnPerformancePreferences(PerformancePreferences newPerformancePreferences){
		returnPerformancePreferences = newPerformancePreferences;		
	}
	
	private static Boolean returnConnectionReuseAddress;
	
	public static void returnConnectionReuseAddress(Boolean newConnectionReuseAddress) {
		returnConnectionReuseAddress = newConnectionReuseAddress;
	}
	
	private static Integer returnConnectionPort;
	
	public static void returnConnectionPort(Integer newConnectionPort) {
		returnConnectionPort = newConnectionPort;
	}
	
	private static Long returnTimeStep;
	
	public static void returnTimeStep(Long newReturnTimeStep) {
		returnTimeStep = newReturnTimeStep;
	}
		
	@Override
	public String getDataStoreName() {
		return wrapped.getDataStoreName();
	}

	@Override
	public String getMacAlgorithm() {
		return wrapped.getMacAlgorithm();
	}

	@Override
	public String getMacProvider() {
		if(returnInvalidMacProvider){
			returnInvalidMacProvider = false;
			return "INVALID";
		}
		
		return wrapped.getMacProvider();
	}

	@Override
	public int getOtpLength() {
		if(returnOtpLength != null){
			return returnOtpLength;
		}
		
		return wrapped.getOtpLength();
	}

	@Override
	public int getSeedLength() {
		return wrapped.getSeedLength();
	}

	@Override
	public String getSecureRandomInstance() {
		if(returnNullSecureRandomGeneratorInstance){
			returnNullSecureRandomGeneratorInstance = false;
			return null;
		}
		
		return wrapped.getSecureRandomInstance();
	}

	@Override
	public String getSecureRandomProvider() {
		if(returnNullSecureRandomGeneratorProvider){
			returnNullSecureRandomGeneratorProvider = false;
			return null;
		}
		
		if(returnInvalidSecureRandomGeneratorProvider){
			returnInvalidSecureRandomGeneratorProvider = false;
			return "INVALID";
		}
		
		return wrapped.getSecureRandomProvider();
	}

	@Override
	public long getTimeStep() {
		if(returnTimeStep != null){
			return returnTimeStep;
		}
		
		return wrapped.getTimeStep();
	}

	@Override
	public long getGracePeriod() {
		return wrapped.getGracePeriod();
	}

	@Override
	public int getMaxAttempts() {
		return wrapped.getMaxAttempts();
	}

	@Override
	public int getLockoutTime() {
		return wrapped.getLockoutTime();
	}

	@Override
	public Map<String, String> getInstalledAdministrators() {
		return wrapped.getInstalledAdministrators();
	}

	@Override
	public String getCountQuery() {
		if(returnCountQuery!=null){
			String toreturn = returnCountQuery;
			returnCountQuery = null;
			return toreturn;
		}
		return wrapped.getCountQuery();
	}

	@Override
	public int getDeviceConnectionPort() {
		if(returnConnectionPort!=null) {
			int toreturn = returnConnectionPort;
			returnConnectionPort = null;
			return toreturn;
		}
		return wrapped.getDeviceConnectionPort();
	}

	@Override
	public int getDeviceConnectionThreads() {
		return wrapped.getDeviceConnectionThreads();
	}

	@Override
	public int getDeviceConnectionReceiveBufferSize() {
		if(returnConnectionReceiveBufferSize !=null) {
			int toreturn = returnConnectionReceiveBufferSize;
			returnConnectionReceiveBufferSize = null;
			return toreturn;
		}
		
		return wrapped.getDeviceConnectionReceiveBufferSize();
	}

	@Override
	public PerformancePreferences getDeviceConnectionPerformancePreferences() {
		if(returnPerformancePreferences != null) {
			PerformancePreferences toreturn = returnPerformancePreferences;
			returnPerformancePreferences = null;
			return toreturn;
		}
		
		return wrapped.getDeviceConnectionPerformancePreferences();
	}

	@Override
	public Boolean getDeviceConnectionReuseAddress() {
		if(returnConnectionReuseAddress != null) {
			Boolean toreturn = returnConnectionReuseAddress;
			returnConnectionReuseAddress = null;
			return toreturn;
		}
		
		return wrapped.getDeviceConnectionReuseAddress();
	}

	@Override
	public int getDeviceConnectionSocketTimeout() {
		return wrapped.getDeviceConnectionSocketTimeout();
	}

	@Override
	public String getDeviceConnectionSSLInstance() {
		return wrapped.getDeviceConnectionSSLInstance();
	}

	@Override
	public String getDeviceConnectionKeystoreType() {
		return wrapped.getDeviceConnectionKeystoreType();
	}

	@Override
	public String getDeviceConnectionKeystoreProvider() {
		return wrapped.getDeviceConnectionKeystoreProvider();
	}

	@Override
	public String getDeviceConnectionKeystore() {
		return wrapped.getDeviceConnectionKeystore();
	}

	@Override
	public String getDeviceConnectionKeystorePassword() {
		return wrapped.getDeviceConnectionKeystorePassword();
	}

	@Override
	public String getDeviceConnectionKeyPassword() {
		return wrapped.getDeviceConnectionKeyPassword();
	}
	
	@Override
	public long getDeviceConnectionEchoInterval() {
		if(returnConnectionEchoInterval != null) {
			Long toreturn = returnConnectionEchoInterval;
			returnConnectionEchoInterval = null;
			return toreturn;
		}
		return wrapped.getDeviceConnectionEchoInterval();
	}
	
	@Override
	public int getExecutorServiceThreads() {
		return wrapped.getExecutorServiceThreads();
	}
	
	@Override
	public long getDeviceConnectionRequestTimeout() {
		if(returnDeviceAuthenticationTimeout != null) {
			Long toreturn = returnDeviceAuthenticationTimeout;
			returnDeviceAuthenticationTimeout = null;
			return toreturn;			
		}
		
		return wrapped.getDeviceConnectionRequestTimeout();
	}
	
	@Override
	public String getServerJwtConfigName() {
		return wrapped.getServerJwtConfigName();
	}

	@Override
	public String getAllowedOrigins() {
		return wrapped.getAllowedOrigins();
	}
	
	@Override
	public String getRequestSigningJwtConfigName() {
		return wrapped.getRequestSigningJwtConfigName();
	}
}
