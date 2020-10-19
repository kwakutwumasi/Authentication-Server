package com.quakearts.auth.server.totp.generator.impl;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.generator.JWTGenerator;
import com.quakearts.auth.server.totp.model.Administrator;
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
	
	private static final Logger log = LoggerFactory.getLogger(JWTGenerator.class);
	
	@Inject
	private TOTPLoginConfiguration totpLoginConfiguration;
	
	@Override
	public String login(Administrator administrator)
			throws NoSuchAlgorithmException, URISyntaxException, LoginException {
		log.debug("Logging in {}", administrator.getCommonName());
		UserPrincipal principal = new UserPrincipal(administrator.getDevice().getId());
		Map<String, Object> sharedState = new HashMap<>();
		sharedState.put("com.quakearts.LoginOk", Boolean.TRUE);
		sharedState.put("javax.security.auth.login.name", principal);
			
		JWTLoginModule jwtLoginModule = new JWTLoginModule();
		jwtLoginModule.initialize(null, DEFUALTCALLBACKHANDLER, sharedState, totpLoginConfiguration.getConfigurationOptions());
		jwtLoginModule.login();
		ArrayList<String[]> roles = new ArrayList<>();
		roles.add(new String[]{"name", administrator.getCommonName()});
		log.debug("Logged in {}", administrator.getCommonName());
		return jwtLoginModule.generateJWTToken(roles);
	}
	
	@Override
	public String generateJWT(Map<String, String> customKeyValues)
			throws JWTException, NoSuchAlgorithmException, URISyntaxException {
		log.debug("Generating JWT for message with hashCode: {}", customKeyValues.hashCode());
		return sign(customKeyValues, totpLoginConfiguration.getServerConfigurationOptions());
	}

	@Override
	public JWTClaims verifyJWT(byte[] jwt)
			throws JWTException, NoSuchAlgorithmException, URISyntaxException {
		log.debug("Verifying JWT for message with hashCode: {}", Arrays.hashCode(jwt));
		return verify(jwt, totpLoginConfiguration.getServerConfigurationOptions());		
	}

	@Override
	public String signRequestJWT(Map<String, String> customKeyValues)
			throws NoSuchAlgorithmException, URISyntaxException, JWTException {
		return sign(customKeyValues, totpLoginConfiguration.getSigningConfigurationOptions());
	}
	
	@Override
	public JWTClaims verifyRequestJWT(byte[] jwt) throws NoSuchAlgorithmException, URISyntaxException, JWTException {
		return verify(jwt, totpLoginConfiguration.getSigningConfigurationOptions());		
	}
	
	private String sign(Map<String, String> customKeyValues, Map<String, ?> options)
			throws JWTException {
		String algorithm = (String) options.get(JWTLoginModule.ALGORITHMPARAMETER);
		
		JWTSigner jwtSigner = JWTFactory.getInstance()
				.getSigner(algorithm, options);
		
		JWTHeader header = JWTFactory.getInstance().createEmptyClaimsHeader();
		JWTClaims claims = JWTFactory.getInstance().createJWTClaimsFromMap(customKeyValues);

		return jwtSigner.sign(header, claims);
	}
	
	private JWTClaims verify(byte[] jwt, Map<String, ?> options) throws JWTException {
		String algorithm = (String) options.get(JWTLoginModule.ALGORITHMPARAMETER);
		
		JWTVerifier jwtVerifier = JWTFactory.getInstance()
				.getVerifier(algorithm, options);
		
		jwtVerifier.verify(jwt);
		return jwtVerifier.getClaims();
	}
	

}
