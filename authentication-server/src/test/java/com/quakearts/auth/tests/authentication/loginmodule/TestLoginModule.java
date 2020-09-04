package com.quakearts.auth.tests.authentication.loginmodule;

import java.io.IOException;
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

import com.quakearts.webapp.security.auth.OtherPrincipal;
import com.quakearts.webapp.security.auth.UserPrincipal;
import com.quakearts.webapp.security.auth.callback.TokenCallback;

public class TestLoginModule implements LoginModule {
	private Subject subject;
	private boolean loginOk;
	private CallbackHandler callbackHandler;
	private String username;
	
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
	}

	@Override
	public boolean login() throws LoginException {
		PasswordCallback passwordCallback = new PasswordCallback("Password", false);
		NameCallback nameCallback = new NameCallback("Name");
		Callback[] callbacks = new Callback[] {
			passwordCallback,
			nameCallback,
			new TokenCallback()
		};
		
		try {
			callbackHandler.handle(callbacks);
		} catch (IOException | UnsupportedCallbackException e) {
			throw new LoginException();
		}		
		
		username = nameCallback.getName();
		
		return (loginOk=Arrays.equals("dGVzdDE=".toCharArray(), passwordCallback.getPassword()));
	}

	@Override
	public boolean commit() throws LoginException {
		if(loginOk) {
			subject.getPrincipals().add(new UserPrincipal(username));
			subject.getPrincipals().add(new OtherPrincipal("value", "test"));
		}
		return loginOk;
	}

	@Override
	public boolean abort() throws LoginException {
		return loginOk;
	}

	@Override
	public boolean logout() throws LoginException {
		return loginOk;
	}

}
