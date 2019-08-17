package com.quakearts.auth.server.totp.exception;

public class SocketShutdownException extends Exception {

	public SocketShutdownException(Exception e) {
		super(e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1432336950951165793L;
}
