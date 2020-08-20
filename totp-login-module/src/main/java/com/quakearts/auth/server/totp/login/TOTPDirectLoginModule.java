package com.quakearts.auth.server.totp.login;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.login.client.TOTPHttpClient;
import com.quakearts.auth.server.totp.login.client.TOTPHttpClientBuilder;
import com.quakearts.auth.server.totp.login.client.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.login.client.model.DirectAuthenticationRequest;
import com.quakearts.auth.server.totp.login.exception.ConnectorException;
import com.quakearts.auth.server.totp.login.exception.LoginOperationException;
import com.quakearts.auth.server.totp.services.DirectAuthenticationService;
import com.quakearts.rest.client.exception.HttpClientException;

public class TOTPDirectLoginModule extends TOTPLoginCommon {
	
	private CallbackHandler callbackHandler;
	private String username;

	private static final Logger log = LoggerFactory.getLogger(TOTPDirectLoginModule.class.getName());
	
	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.options = (Map<String, String>) options;
		this.callbackHandler = callbackHandler;
	}

	@Override
	public boolean login() throws LoginException {
		try {
			processCallbacks();
			processDirectAuthentication();
		} catch (LoginOperationException | HttpClientException | IOException e) {
			log.error("Unable to process login", e);
			if(Boolean.parseBoolean(options.getOrDefault("fail.on.error", "true"))){
				throw new LoginException(e.getMessage());
			}
			return false;
		}
		
		return true;
	}

	private void processDirectAuthentication()
			throws IOException, HttpClientException {
		TOTPHttpClient client = TOTPHttpClientBuilder
				.createTOTPServerHttpClient(options.getOrDefault("totp.server.url", "http://localhost:8080/totp"));
		DirectAuthenticationService service = DirectAuthenticationService.getInstance();
		try {
			client.authenticateDirectly(new DirectAuthenticationRequest()
					.setDeviceIdAs(username)
					.addAuthenticationData("Application Name", options.getOrDefault("application.name", "Application"))
					.addAuthenticationData("Authentication ID", service.generateID(username)));
		} catch (ConnectorException e) {
			if(e.getHttpCode()==404 && Boolean.parseBoolean(options.getOrDefault("allow.fallback", "false"))){
				client.authenticate(new AuthenticationRequest().setDeviceIdAs(username)
						.setOtpAs(service.getFallbackToken(username)));
			} else {
				throw e;
			}
		} finally {
			service.removeID(username);
		}
		loginOk = true;
	}

	private void processCallbacks() throws LoginOperationException {
		NameCallback name = new NameCallback("Enter your username","anonymous");
		Callback[] callbacks = new Callback[1];
		callbacks[0] = name;

		try {
			callbackHandler.handle(callbacks);
		} catch (IOException | UnsupportedCallbackException e) {
			throw new LoginOperationException("Callback could not be processed", e);
		}

		username = (name.getName() == null ? name.getDefaultName(): name.getName()).trim();		
	}

	@Override
	public boolean abort() throws LoginException {
		return logout();
	}

	@Override
	public boolean logout() throws LoginException {
		username = null;
		loginOk = false;
		subject = null;
		return true;
	}

}
