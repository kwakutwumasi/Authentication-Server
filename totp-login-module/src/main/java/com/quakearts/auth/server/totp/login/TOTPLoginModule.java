package com.quakearts.auth.server.totp.login;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.login.client.TOTPHttpClient;
import com.quakearts.auth.server.totp.login.client.TOTPHttpClientBuilder;
import com.quakearts.auth.server.totp.login.client.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.login.exception.ConnectorException;
import com.quakearts.auth.server.totp.login.exception.LoginOperationException;
import com.quakearts.rest.client.exception.HttpClientException;

public class TOTPLoginModule extends TOTPLoginCommon {
	private static final Logger log = LoggerFactory.getLogger(TOTPLoginModule.class.getName());
	
	private CallbackHandler callbackHandler;
	private Map<String, ?> sharedState;
	private String username;
	private String password;
	private char[] sharedPasswordChars = new char[0];
	@SuppressWarnings("unchecked")
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = sharedState;
		this.options = (Map<String, String>) options;
	}

	public boolean login() throws LoginException {
		try {
			processCallbacks();
			runPreAuthenticationChecks();
			processAuthentication();
			performFinalActions();
		} catch (LoginOperationException e) {
			log.error("Login processing failed due to an internal error", e);
			if(Boolean.parseBoolean(options.getOrDefault("fail.on.error", "true"))){
				throw new LoginException(e.getMessage());
			}
		} finally {
			password = null;
		}
		return loginOk;
	}

	private void processCallbacks() throws LoginOperationException {
		NameCallback name = new NameCallback("Enter your username","anonymous");
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
		if(pass.getPassword() != null){
			char[] passwordChars = pass.getPassword();
			int otpLength = getOtpLength();
			if(passwordChars.length > otpLength){
				password = new String(passwordChars, passwordChars.length-otpLength, otpLength);
				sharedPasswordChars = new char[passwordChars.length-otpLength];
				System.arraycopy(passwordChars, 0, sharedPasswordChars, 0, passwordChars.length-otpLength);
			}
		}
	}

	private int getOtpLength() {
		return Integer.parseInt(options.getOrDefault("totp.token.length", "6"));
	}

	private void runPreAuthenticationChecks() throws LoginException {
		if (password == null)
			throw new LoginException("Username/Password is null.");
	}

	private void processAuthentication() 
				throws LoginException, LoginOperationException {
		TOTPHttpClient client = TOTPHttpClientBuilder
				.createTOTPServerHttpClient(options.getOrDefault("totp.server.url", "http://localhost:8080/totp"));
		try {
			client.authenticate(new AuthenticationRequest().setDeviceIdAs(username).setOtpAs(password));
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
		if (sharedState != null) {
			TOTPDevicePrincipal shareduser = new TOTPDevicePrincipal(username);
			@SuppressWarnings("unchecked")
			Map<String, Object> sharedStateObj = ((Map<String, Object>)sharedState);
			sharedStateObj.put("javax.security.auth.login.name", shareduser);
			sharedStateObj.put("javax.security.auth.login.password", sharedPasswordChars);
		}
		loginOk = true;
	}

	public boolean abort() throws LoginException {
		return logout();
	}

	public boolean logout() throws LoginException {
		username = null;
		password = null;
		sharedPasswordChars = new char[0];
		loginOk = false;
		subject = null;
		return true;
	}

}
