package com.quakearts.auth.server.totp.login;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public abstract class TOTPLoginCommon implements LoginModule {

	protected Map<String, String> options;
	protected boolean loginOk;
	protected Subject subject;

	public boolean commit() throws LoginException {
		if(loginOk) {
			Principal principal = new TOTPDevicePrincipal("TOTP-Authenticated");
			subject.getPrincipals().add(principal);
		}
		return loginOk;
	}
}