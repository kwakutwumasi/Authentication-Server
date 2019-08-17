package com.quakearts.auth.server.totp.exception;

public class MissingNameException extends TOTPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5733441884724524560L;
	
	@Override
	protected String getMessageInternal() {
		return "The administrator name is required";
	}
	
	@Override
	protected int getHttpCode() {
		return 400;
	}
}
