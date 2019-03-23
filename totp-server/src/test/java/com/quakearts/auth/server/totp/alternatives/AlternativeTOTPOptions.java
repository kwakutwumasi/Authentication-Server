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
	
	@Inject
	private TOTPOptionsImpl wrapped;
	
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
		if(returnOtpLength!=null){
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
}
