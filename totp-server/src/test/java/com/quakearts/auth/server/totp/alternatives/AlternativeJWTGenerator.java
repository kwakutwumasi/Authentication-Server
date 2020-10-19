package com.quakearts.auth.server.totp.alternatives;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;
import javax.security.auth.login.LoginException;

import com.quakearts.auth.server.totp.generator.JWTGenerator;
import com.quakearts.auth.server.totp.generator.impl.JWTGeneratorImpl;
import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.exception.JWTException;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@Singleton
public class AlternativeJWTGenerator implements JWTGenerator {

	@Inject
	private JWTGeneratorImpl wrapped;
	private static Exception throwGenerateError;
	
	public static void throwGenerateError(Exception newThrowGenerateError) {
		throwGenerateError = newThrowGenerateError;
	}
	
	private static Exception throwVerifyError;
	
	public static void throwVerifyError(Exception newThrowVerifyError) {
		throwVerifyError = newThrowVerifyError;
	}
	
	private static Exception throwSignatureError;
	
	public static void throwSignatureError(Exception newThrowSignatureError) {
		throwSignatureError = newThrowSignatureError;
	}
	
	private static Exception throwSignatureVerifyError;
	
	public static void throwSignatureVerifyError(Exception newThrowSignatureVerifyError) {
		throwSignatureVerifyError = newThrowSignatureVerifyError;
	}
	
	@Override
	public String generateJWT(Map<String, String> customKeyValues)
			throws NoSuchAlgorithmException, URISyntaxException, JWTException {
		checkAndThrowGenerateErrorIfNecessary();
		return wrapped.generateJWT(customKeyValues);
	}

	@Override
	public JWTClaims verifyJWT(byte[] jwt) throws NoSuchAlgorithmException, URISyntaxException, JWTException {
		checkAndThrowVerifyErrorIfNecessary();
		return wrapped.verifyJWT(jwt);
	}
	
	@Override
	public String signRequestJWT(Map<String, String> customKeyValues)
			throws NoSuchAlgorithmException, URISyntaxException, JWTException {
		checkAndThrowSignatureErrorIfNecessary();
		return wrapped.signRequestJWT(customKeyValues);
	}
	
	@Override
	public JWTClaims verifyRequestJWT(byte[] jwt) throws NoSuchAlgorithmException, URISyntaxException, JWTException {
		checkAndThrowSignatureVerifyErrorIfNecessary();
		return wrapped.verifyJWT(jwt);
	}
	
	@Override
	public String login(Administrator administrator) throws NoSuchAlgorithmException, URISyntaxException, LoginException {
		return wrapped.login(administrator);
	}
	
	private void checkAndThrowGenerateErrorIfNecessary() throws NoSuchAlgorithmException, URISyntaxException, JWTException {
		if(throwGenerateError != null) {
			Exception toThrow = throwGenerateError;
			throwGenerateError = null;
			throwException(toThrow);
		}
	}
	
	private void checkAndThrowVerifyErrorIfNecessary() throws NoSuchAlgorithmException, URISyntaxException, JWTException {
		if(throwVerifyError != null) {
			Exception toThrow = throwVerifyError;
			throwVerifyError = null;
			throwException(toThrow);
		}
	}

	private void checkAndThrowSignatureErrorIfNecessary() throws NoSuchAlgorithmException, URISyntaxException, JWTException {
		if(throwSignatureError != null) {
			Exception toThrow = throwSignatureError;
			throwSignatureError = null;
			throwException(toThrow);
		}
	}
	
	private void checkAndThrowSignatureVerifyErrorIfNecessary() throws NoSuchAlgorithmException, URISyntaxException, JWTException {
		if(throwSignatureVerifyError != null) {
			Exception toThrow = throwSignatureVerifyError;
			throwSignatureVerifyError = null;
			throwException(toThrow);
		}
	}

	private void throwException(Exception toThrow) throws NoSuchAlgorithmException, URISyntaxException, JWTException {
		if(toThrow instanceof NoSuchAlgorithmException) {
			throw (NoSuchAlgorithmException) toThrow;
		} else if(toThrow instanceof URISyntaxException) {
			throw (URISyntaxException) toThrow;
		} else if(toThrow instanceof JWTException) {
			throw (JWTException) toThrow;
		}
	}

}
