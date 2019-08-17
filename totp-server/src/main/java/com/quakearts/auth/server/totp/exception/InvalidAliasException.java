package com.quakearts.auth.server.totp.exception;

public class InvalidAliasException extends TOTPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2965656795887924924L;

	@Override
	protected String getMessageInternal() {
		return "The alias supplied is not valid";
	}

	@Override
	protected int getHttpCode() {
		return 400;
	}
}
