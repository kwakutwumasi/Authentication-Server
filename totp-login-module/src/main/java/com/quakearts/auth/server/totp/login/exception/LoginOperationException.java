package com.quakearts.auth.server.totp.login.exception;

public class LoginOperationException extends Exception {

	public LoginOperationException(String message, Exception throwable) {
		super(message, throwable);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 6602135395652233234L;

}
