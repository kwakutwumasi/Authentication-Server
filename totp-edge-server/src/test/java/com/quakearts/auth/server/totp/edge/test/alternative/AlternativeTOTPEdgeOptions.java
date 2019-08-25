package com.quakearts.auth.server.totp.edge.test.alternative;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.options.impl.TOTPEdgeOptionsImpl;

@Alternative
@Singleton
@Priority(Interceptor.Priority.APPLICATION)
public class AlternativeTOTPEdgeOptions extends TOTPEdgeOptionsImpl {
	
	private static boolean returnInvalidAlgorithm;
	
	public static void returnInvalidAlgorithm(boolean newInvalidAlgorithm) {
		returnInvalidAlgorithm = newInvalidAlgorithm;
	}
	
	@Override
	public String getJwtalgorithm() {
		if(returnInvalidAlgorithm) {
			returnInvalidAlgorithm = false;
			return "";
		}
		return super.getJwtalgorithm();
	}
}
