package com.quakearts.auth.server.totp.exception;

public class InvalidDeviceStatusException extends TOTPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7129842831996553835L;
	
	@Override
	protected String getMessageInternal() {
		return "The device is not in a valid state for provisioning";
	}

	@Override
	protected int getHttpCode() {
		return 401;
	}
}
