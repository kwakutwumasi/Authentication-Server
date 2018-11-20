package com.quakearts.auth.server.rest.models;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

public class TokenResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 82489519046018710L;
	@Schema(description="The type of token. This is currently set to Bearer, and may change later")
	private String tokenType;
	@Schema(description="The number of seconds that will elapse before the token expires. "
			+ "This is useful for keeping track of when the token will expire.")
	private long expiresIn;
	@Schema(description="The security token in JWT format.")
	private String idToken;

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}
	
	public TokenResponse withTokenTypeAs(String tokenType) {
		setTokenType(tokenType);
		return this;
	}

	public long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(long expiresIn) {
		this.expiresIn = expiresIn;
	}
	
	public TokenResponse withExpiresInAs(long expiresIn) {
		setExpiresIn(expiresIn);
		return this;
	}

	public String getIdToken() {
		return idToken;
	}

	public void setIdToken(String idToken) {
		this.idToken = idToken;
	}

	public TokenResponse withIdTokenAs(String idToken) {
		setIdToken(idToken);
		return this;
	}
}
