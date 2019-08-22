package com.quakearts.auth.server.totp.login;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.quakearts.auth.server.totp.login.client.TOTPHttpClient;
import com.quakearts.auth.server.totp.login.client.TOTPHttpClientBuilder;
import com.quakearts.auth.server.totp.login.client.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.login.exception.ConnectorException;
import com.quakearts.auth.server.totp.login.exception.LoginOperationException;
import com.quakearts.rest.client.exception.HttpClientException;

public class TOTPLoginModule implements LoginModule {
	private static final Logger log = Logger.getLogger(TOTPLoginModule.class.getName());
	
	private Subject subject;
	private CallbackHandler callbackHandler;
	private Map<String, ?> sharedState;
	private boolean loginOk;
	private String username;
	private String password;
	private String serverUrl;
	
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = sharedState;
		
		@SuppressWarnings("unchecked")
		Map<String, String> optionsCast = (Map<String, String>) options;
		serverUrl = optionsCast.computeIfAbsent("totp.url", test->"http://localhost:8080/totp");
	}

	public boolean login() throws LoginException {
		try {
			processCallbacks();
			runPreAuthenticationChecks();
			processAuthentication();
			performFinalActions();
		} catch (LoginOperationException e) {
			log.log(Level.SEVERE, "Login processing failed due to an internal error", e);
			return false;
		} finally {
			password = null;
		}
		return loginOk;
	}

	private void processCallbacks() throws LoginOperationException {
		NameCallback name = new NameCallback("Enter your username","annonymous");
		PasswordCallback pass = new PasswordCallback("Enter your password:", false);
		Callback[] callbacks = new Callback[2];
		callbacks[0] = name;
		callbacks[1] = pass;
	
		try {
			callbackHandler.handle(callbacks);
		} catch (IOException | UnsupportedCallbackException e) {
			throw new LoginOperationException("Callback could not be processed", e);
		}
	
		username = (name.getName() == null ? name.getDefaultName(): name.getName()).trim();
		if(pass.getPassword() != null)
			password = new String(pass.getPassword());
	}

	private void runPreAuthenticationChecks() throws LoginException {
		if (password == null)
			throw new LoginException("Username/Password is null.");
	}

	private void processAuthentication() 
				throws LoginException, LoginOperationException {
		TOTPHttpClient client = TOTPHttpClientBuilder
				.createTOTPServerHttpClient(serverUrl);
		try {
			if(username.equals(password)) {
				client.authenticationDirect(username);
			} else {
				AuthenticationRequest request = new AuthenticationRequest();
				request.setDeviceId(username);
				request.setOtp(password);
				client.authentication(request);
			}
		} catch (ConnectorException e) {
			if(e.getHttpCode() == 403) {
				throw new LoginException(e.getResponse().getMessage());
			} else {
				throw new LoginOperationException(e.getResponse().getMessage(), e);
			}
		} catch (IOException | HttpClientException e) {
			throw new LoginOperationException("Client is unable to connect for authentication", e);
		}
	}

	private void performFinalActions() {
		loginOk = true;
		if (sharedState != null) {
			TOTPDevicePrincipal shareduser = new TOTPDevicePrincipal(username);
			@SuppressWarnings("unchecked")
			Map<String, Object> sharedStateObj = ((Map<String, Object>)sharedState);
			sharedStateObj.put("javax.security.auth.login.name", shareduser);
			sharedStateObj.put("javax.security.auth.login.password", password.toCharArray());
			sharedStateObj.put("com.quakearts.LoginOk", loginOk);					
		}
	}

	public boolean commit() throws LoginException {
		if(loginOk)
			subject.getPrincipals().add(new TOTPDevicePrincipal(username));
		return loginOk;
	}

	public boolean abort() throws LoginException {
		return logout();
	}

	public boolean logout() throws LoginException {
		loginOk = false;
		return true;
	}

}
