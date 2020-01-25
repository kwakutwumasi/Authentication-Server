package com.quakearts.auth.server.totp.alternatives;

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

}
