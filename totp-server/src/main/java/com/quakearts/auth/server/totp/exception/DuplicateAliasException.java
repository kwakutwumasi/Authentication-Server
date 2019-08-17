package com.quakearts.auth.server.totp.exception;

public class DuplicateAliasException extends TOTPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8297430795382536561L;
	
	@Override
	protected String getMessageInternal() {
		return "The alias supplied has already been assigned";
	}
	
	@Override
	protected int getHttpCode() {
		return 400;
	}
}
