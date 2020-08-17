package com.quakearts.auth.server.totp.login.testmain;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.quakearts.auth.server.totp.login.TOTPDevicePrincipal;

public class TestLoginModule implements LoginModule {

	private Subject subject;
	private String username;
	private Map<String, ?> sharedState;
	private CallbackHandler callbackHandler;
	private boolean loginOk;
	
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.sharedState = sharedState;
		this.callbackHandler = callbackHandler;
	}

	@Override
	public boolean login() throws LoginException {
		char[] password;
		try {
			username = ((Principal)sharedState.get("javax.security.auth.login.name")).getName();
			password = (char[])sharedState.get("javax.security.auth.login.password");
		} catch (Exception e) {
			Callback[] callbacks = new Callback[]{new NameCallback("Enter username:"),
					new PasswordCallback("Enter password", false)};
			try {
				callbackHandler.handle(callbacks);
			} catch (IOException | UnsupportedCallbackException e1) {
				throw new LoginException(e1.getMessage());
			}
			username = ((NameCallback)callbacks[0]).getName();
			password = ((PasswordCallback)callbacks[1]).getPassword();
		}
		
		if(!Arrays.equals("password".toCharArray(), password)){
			throw new LoginException("Invalid password");
		}
		
		return loginOk=true;
	}

	@Override
	public boolean commit() throws LoginException {
		if(loginOk){
			subject.getPrincipals().add(new TOTPDevicePrincipal(username));
			subject.getPrincipals().add(new TestPrincipal());
		}
		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		return logout();
	}

	@Override
	public boolean logout() throws LoginException {
		loginOk = false;
		return true;
	}

}
