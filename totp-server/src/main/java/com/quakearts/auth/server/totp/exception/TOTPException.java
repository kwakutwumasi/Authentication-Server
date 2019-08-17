package com.quakearts.auth.server.totp.exception;

public abstract class TOTPException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3605822259159745664L;

	protected abstract String getMessageInternal();
	protected abstract int getHttpCode();
	
	public TOTPException() {
		super();
	}
	
	public TOTPException(Throwable t) {
		super(t);
	}
	
	@Override
	public String getMessage() {
		return getMessageInternal();
	}
}
