package com.quakearts.auth.server.totp.exception;

public class MessageGenerationException extends TOTPException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3735010285182104128L;

	public MessageGenerationException(Throwable t) {
		super(t);
	}

	@Override
	protected String getMessageInternal() {
		return "Message generation failed. " + getCause().getMessage();
	}

	@Override
	protected int getHttpCode() {
		return 500;
	}
}
