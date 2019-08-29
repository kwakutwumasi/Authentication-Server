package com.quakearts.auth.server.totp.exception;

public class CallbackException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2546338681877570835L;

	public CallbackException(Throwable t) {
		super(t);
	}

}
