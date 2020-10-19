package com.quakearts.auth.server.totp.alternatives;

import java.text.MessageFormat;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.generator.TOTPGenerator;
import com.quakearts.auth.server.totp.generator.impl.TOTPGeneratorImpl;
import com.quakearts.auth.server.totp.model.Device;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@Singleton
public class AlternativeTOTPGenerator implements TOTPGenerator {

	private static boolean simulate;
	
	public static void simulate(boolean newSimulate){
		simulate = newSimulate;
	}
	
	private static String expectedRequest;
	
	public static void expectedRequest(String newExpectedRequest){
		expectedRequest = newExpectedRequest;
	}
	
	@Inject
	private TOTPGeneratorImpl wrapped;
	
	@Override
	public String[] generateFor(Device device, long currentTimeInMillis) {
		if(simulate){
			simulate = false;
			return device.getId().equals("generateone")?new String[]{"123456", null}:new String[]{"123456","789101"};
		} else {
			return wrapped.generateFor(device, currentTimeInMillis);
		}
	}

	@Override
	public String signRequest(Device device, String request) {
		if(simulate){
			simulate = false;
			if(!request.equals(expectedRequest))
				throw new AssertionError(MessageFormat.format("Expected {0}. got {1}", expectedRequest, request));
			
			return "52ee0d38528929f5473109bf7998aeecd29ab6bddf6063888786e59d0228bb3c";
		} else {
			return wrapped.signRequest(device, request);
		}
	}

}
