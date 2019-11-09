package com.quakearts.auth.server.totp.login;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.totp.login.client.TOTPHttpClient;
import com.quakearts.auth.server.totp.login.client.TOTPHttpClientBuilder;
import com.quakearts.auth.server.totp.login.client.model.AuthenticationRequest;
import com.quakearts.rest.client.exception.HttpClientException;
import com.quakearts.tools.test.mockserver.MockServer;
import com.quakearts.tools.test.mockserver.MockServerFactory;
import com.quakearts.tools.test.mockserver.configuration.Configuration.MockingMode;
import com.quakearts.tools.test.mockserver.configuration.impl.ConfigurationBuilder;
import com.quakearts.tools.test.mockserver.model.impl.HttpMessageBuilder;
import com.quakearts.tools.test.mockserver.model.impl.MockActionBuilder;

public class TOTPLoginModuleTest {

	//all ok otp
	//all ok direct

	private static MockServer mockServer;
	private static TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String,Object>>() {
	};
	
	private static ObjectMapper mapper = new ObjectMapper();
	
	@BeforeClass
	public static void startServer() {
		mockServer = MockServerFactory
				.getInstance()
				.getMockServer()
				.configure(ConfigurationBuilder
						.newConfiguration().
						setMockingModeAs(MockingMode.MOCK)
						.thenBuild())
				.add(MockActionBuilder.createNewMockAction()
					.setRequestAs(HttpMessageBuilder.createNewHttpRequest()
						.setId("otp-authentication")
						.setMethodAs("POST")
						.setResourceAs("/totp/authenticate")
							.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
								.setResponseCodeAs(204)
								.thenBuild())
							.thenBuild())
					.setResponseActionAs((request, response)->{
						if(request.getContentBytes() == null) {
							return HttpMessageBuilder
									.createNewHttpResponse().setResponseCodeAs(500)
									.thenBuild();
						}
						
						try {
							Map<String, Object> jsonRequest = mapper
									.readValue(request.getContentBytes(), typeReference);
							if(!jsonRequest.containsKey("otp") || 
									!jsonRequest.get("otp").equals("123456")) {
								return HttpMessageBuilder
										.createNewHttpResponse().setResponseCodeAs(500)
										.thenBuild();
							}
							
							switch (jsonRequest.get("deviceId").toString()) {
							case "testdevice-ok":
								return response;
							case "testdevice-not-found":
								return HttpMessageBuilder
										.createNewHttpResponse().setResponseCodeAs(404)
										.setContentBytes("{\"message\":\"Error-not-found\"}".getBytes())
										.thenBuild();
							case "testdevice-deserialize-error":
								return HttpMessageBuilder
										.createNewHttpResponse().setResponseCodeAs(404)
										.setContentBytes("{\"message\":\"Error-deserialize-error".getBytes())
										.thenBuild();
							default:
								return HttpMessageBuilder
										.createNewHttpResponse().setResponseCodeAs(403)
										.setContentBytes("{\"message\":\"Error-uknown\"}".getBytes())
										.thenBuild();
							}
						} catch (IOException e) {
							return HttpMessageBuilder
									.createNewHttpResponse().setResponseCodeAs(500)
									.thenBuild();
						}
					})
					.thenBuild())
				.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(HttpMessageBuilder.createNewHttpRequest()
								.setId("direct-authentication")
								.setMethodAs("GET")
								.setResourceAs("/totp/authenticate/device/testDirect")
									.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
										.setResponseCodeAs(204)
										.thenBuild())
									.thenBuild())
							.setResponseActionAs((request, response)->{
								
								return null;
							})
					.thenBuild());
		
		mockServer.start();
	}
	
	@AfterClass
	public static void shutdown() {
		mockServer.stop();
	}
	
	@Test
	public void testAllOkAuthenticate() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = (callbacks)->{
			for(Callback callback:callbacks) {
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("123456".toCharArray());
				} else if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-ok");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
		
		options.put("totp.url", "http://localhost:8080/totp");
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		assertThat(loginModule.login(),is(true));
		assertThat(loginModule.commit(),is(true));
		assertThat(loginModule.logout(),is(true));
		assertThat(loginModule.abort(),is(true));
		
		TOTPDevicePrincipal devicePrincipal = subject
				.getPrincipals(TOTPDevicePrincipal.class).iterator().next();
		
		assertThat(devicePrincipal, is(notNullValue()));
		assertThat(devicePrincipal.equals(null), is(false));
		assertThat(((Object)devicePrincipal).equals(""), is(false));
		assertThat(devicePrincipal.equals(devicePrincipal), is(true));

		assertThat(devicePrincipal.getName(),is("testdevice-ok"));
		
		assertThat(devicePrincipal.equals(new TOTPDevicePrincipal("testdevice-ok")), is(true));
		assertThat(devicePrincipal.equals(new TOTPDevicePrincipal(null)), is(false));
		assertThat(new TOTPDevicePrincipal(null).equals(new TOTPDevicePrincipal(null)), is(true));
		assertThat(new TOTPDevicePrincipal(null).equals(devicePrincipal), is(false));
		
		assertThat(new TOTPDevicePrincipal(null).hashCode(), is(31));
		
		assertThat(sharedstate.get("javax.security.auth.login.name"), is(devicePrincipal));
		assertThat(sharedstate.get("javax.security.auth.login.password"), is("123456".toCharArray()));
		assertThat(sharedstate.get("com.quakearts.LoginOk"), is(true));
	}
	
	@Test
	public void testAllOkAuthenticateDirect() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = (callbacks)->{
			for(Callback callback:callbacks) {
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("testDirect".toCharArray());
				} else if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testDirect");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		assertThat(loginModule.login(),is(true));
		assertThat(loginModule.commit(),is(true));
		
		TOTPDevicePrincipal devicePrincipal = subject
				.getPrincipals(TOTPDevicePrincipal.class).iterator().next();
		
		assertThat(devicePrincipal, is(notNullValue()));
		assertThat(devicePrincipal.equals(null), is(false));
		assertThat(((Object)devicePrincipal).equals(""), is(false));
		assertThat(devicePrincipal.equals(devicePrincipal), is(true));
		assertThat(devicePrincipal.equals(new TOTPDevicePrincipal("testDirect")), is(true));
	}

	//callbackhandler with IOException
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testCallbackhandlerIOException() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = (callbacks)->{
			throw new IOException("Error thrown");
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		assertThat(loginModule.login(),is(false));
		assertThat(loginModule.commit(),is(false));
	}
	
	//callbackhandler with UnsupportedCallbackException
	
	@Test
	public void testCallbackhandlerUnsupportedCallbackException() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = (callbacks)->{
			throw new UnsupportedCallbackException(callbacks[0]);
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		assertThat(loginModule.login(),is(false));
		assertThat(loginModule.commit(),is(false));
	}

	
	//callback with default name
	//Connector error 403
	
	@Test
	public void testCallbackhandlerWithDefaultName() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage("Error-uknown");
		Subject subject = new Subject();
		CallbackHandler callbackHandler = (callbacks)->{
			for(Callback callback:callbacks) {
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("123456".toCharArray());
				} 
			}	
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		loginModule.login();
	}
	
	//password null
	
	@Test
	public void testEmptyCallbackhandler() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage("Username/Password is null.");
		Subject subject = new Subject();
		CallbackHandler callbackHandler = (callbacks)->{};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		assertThat(loginModule.login(),is(false));
		assertThat(loginModule.commit(),is(false));
	}
	
	//Connector error 404
	
	@Test
	public void testConnectorError404() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = (callbacks)->{
			for(Callback callback:callbacks) {
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("123456".toCharArray());
				} else if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-not-found");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		assertThat(loginModule.login(),is(false));
		assertThat(loginModule.commit(),is(false));
	}
	
	//IOException
	
	@Test
	public void testIOException() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = (callbacks)->{
			for(Callback callback:callbacks) {
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("123456".toCharArray());
				} else if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-not-found");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
		
		options.put("totp.url", "http://localhost:8000/totp");
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		assertThat(loginModule.login(),is(false));
		assertThat(loginModule.commit(),is(false));
	}
	
	@Test
	public void testJSONException() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = (callbacks)->{
			for(Callback callback:callbacks) {
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("123456".toCharArray());
				} else if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-deserialize-error");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
				
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		assertThat(loginModule.login(),is(false));
		assertThat(loginModule.commit(),is(false));
	}
	
	@Test
	public void testTOTPClientHTTPSerializationError() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage("Unable to serialize object");
		TOTPHttpClient client = TOTPHttpClientBuilder
				.createTOTPServerHttpClient("http://localhost:8080/totp");
		
		client.authentication(new AuthenticationRequestBomb());
	}
	
	class AuthenticationRequestBomb 
		extends AuthenticationRequest {
		@Override
		public String getDeviceId() {
			throw new UnsupportedOperationException();
		}
	}
}
