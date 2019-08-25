package com.quakearts.auth.server.totp.exception;

public class InvalidInputException extends TOTPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2174275356451635360L;

	@Override
	protected String getMessageInternal() {
		return null;
	}

	@Override
	protected int getHttpCode() {
		return 500;
	}

}
