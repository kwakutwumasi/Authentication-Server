package com.quakearts.auth.server.totp.exception;

public class UnconnectedDeviceException extends TOTPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3478385432097513571L;
	private final String additionalInfo;

	public UnconnectedDeviceException(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	@Override
	protected String getMessageInternal() {
		return "The specified device is not connected. "+additionalInfo;
	}

	@Override
	protected int getHttpCode() {
		return 404;
	}

}
