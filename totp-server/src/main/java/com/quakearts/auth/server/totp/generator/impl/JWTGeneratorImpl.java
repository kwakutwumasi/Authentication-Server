package com.quakearts.auth.server.totp.generator.impl;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import com.quakearts.auth.server.totp.generator.JWTGenerator;
import com.quakearts.auth.server.totp.options.TOTPLoginConfiguration;
import com.quakearts.webapp.security.auth.JWTLoginModule;
import com.quakearts.webapp.security.auth.UserPrincipal;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.JWTHeader;
import com.quakearts.webapp.security.jwt.JWTSigner;
import com.quakearts.webapp.security.jwt.JWTVerifier;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.webapp.security.jwt.factory.JWTFactory;

@Singleton
public class JWTGeneratorImpl implements JWTGenerator {
	private static final CallbackHandler DEFUALTCALLBACKHANDLER = callbacks->{};
	@Inject
	private TOTPLoginConfiguration totpLoginConfiguration;
	
	@Override
	public String login(String username)
			throws NoSuchAlgorithmException, URISyntaxException, LoginException {
		UserPrincipal principal = new UserPrincipal(username);
		Map<String, Object> sharedState = new HashMap<>();
		sharedState.put("com.quakearts.LoginOk", Boolean.TRUE);
		sharedState.put("javax.security.auth.login.name", principal);
			
		JWTLoginModule jwtLoginModule = new JWTLoginModule();
		jwtLoginModule.initialize(null, DEFUALTCALLBACKHANDLER, sharedState, totpLoginConfiguration.getConfigurationOptions());
		jwtLoginModule.login();
		
		return jwtLoginModule.generateJWTToken(Collections.emptyList());
	}
	
	@Override
	public String generateJWT(Map<String, String> customKeyValues)
			throws JWTException, NoSuchAlgorithmException, URISyntaxException {
		String algorithm = (String) totpLoginConfiguration.getServerConfigurationOptions()
				.get(JWTLoginModule.ALGORITHMPARAMETER);
		
		JWTSigner jwtSigner = JWTFactory.getInstance()
				.getSigner(algorithm, totpLoginConfiguration.getServerConfigurationOptions());
		
		JWTHeader header = JWTFactory.getInstance().createEmptyClaimsHeader();
		JWTClaims claims = JWTFactory.getInstance().createJWTClaimsFromMap(customKeyValues);
		
		return jwtSigner.sign(header, claims);
	}
	
	@Override
	public JWTClaims verifyJWT(byte[] jwt)
			throws JWTException, NoSuchAlgorithmException, URISyntaxException {
		String algorithm = (String) totpLoginConfiguration.getServerConfigurationOptions()
				.get(JWTLoginModule.ALGORITHMPARAMETER);
		
		JWTVerifier jwtVerifier = JWTFactory.getInstance()
				.getVerifier(algorithm, totpLoginConfiguration.getServerConfigurationOptions());
		
		jwtVerifier.verify(jwt);
		
		return jwtVerifier.getClaims();
	}
}
