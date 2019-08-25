package com.quakearts.auth.server.totp.exception;

public class AuthenticationException extends TOTPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7314842547997777746L;
	private final String additionalMessage;
	
	public AuthenticationException(String additionalMessage) {
		this.additionalMessage = additionalMessage;
	}

	@Override
	protected String getMessageInternal() {
		return additionalMessage;
	}

	@Override
	protected int getHttpCode() {
		return 403;
	}

}
