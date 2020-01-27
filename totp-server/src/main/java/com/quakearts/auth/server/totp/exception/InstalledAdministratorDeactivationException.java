package com.quakearts.auth.server.totp.exception;

public class InstalledAdministratorDeactivationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1391543838013524171L;

	public InstalledAdministratorDeactivationException() {
		super("The device is an installed administrator and cannot be deactivated");
	}

}
