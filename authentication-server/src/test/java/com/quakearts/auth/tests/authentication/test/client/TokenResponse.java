package com.quakearts.auth.tests.authentication.test.client;

import java.io.Serializable;

public class TokenResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 82489519046018710L;
	private String tokenType;
	private long expiresIn;
	private String idToken;

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}
	
	public TokenResponse setTokenTypeAs(String tokenType) {
		setTokenType(tokenType);
		return this;
	}

	public long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(long expiresIn) {
		this.expiresIn = expiresIn;
	}
	
	public TokenResponse setExpiresInAs(long expiresIn) {
		setExpiresIn(expiresIn);
		return this;
	}

	public String getIdToken() {
		return idToken;
	}

	public void setIdToken(String idToken) {
		this.idToken = idToken;
	}

	public TokenResponse setIdTokenAs(String idToken) {
		setIdToken(idToken);
		return this;
	}
}
