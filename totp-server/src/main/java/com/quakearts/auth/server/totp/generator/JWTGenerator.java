package com.quakearts.auth.server.totp.generator;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.security.auth.login.LoginException;

import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.exception.JWTException;

public interface JWTGenerator {
	String generateJWT(Map<String, String> customKeyValues) throws NoSuchAlgorithmException, URISyntaxException, JWTException;
	JWTClaims verifyJWT(byte[] jwt) throws NoSuchAlgorithmException, URISyntaxException, JWTException;
	String login(String username) throws NoSuchAlgorithmException, URISyntaxException, LoginException;
}
