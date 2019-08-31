package com.quakearts.auth.server.totp.exception;

public class InvalidSignatureException extends TOTPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6175991955497834670L;
	private final String additionalReason;
	
	public InvalidSignatureException(String additionalReason) {
		this.additionalReason = additionalReason;
	}

	public InvalidSignatureException(Throwable t) {
		super(t);
		additionalReason = t.getMessage();
	}

	@Override
	protected String getMessageInternal() {
		return "The provided signature could not be validated: "+additionalReason;
	}

	@Override
	protected int getHttpCode() {
		return 400;
	}

}
