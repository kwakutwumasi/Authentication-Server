package com.quakearts.auth.server.totp.login.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.awaitility.Awaitility.*;

import java.io.IOException;
import java.security.acl.Group;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.awaitility.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.quakearts.auth.server.totp.login.RolesGroup;
import com.quakearts.auth.server.totp.login.TOTPDevicePrincipal;
import com.quakearts.auth.server.totp.login.TOTPDirectLoginModule;
import com.quakearts.auth.server.totp.login.client.TOTPHttpClient;
import com.quakearts.auth.server.totp.login.client.TOTPHttpClientBuilder;
import com.quakearts.auth.server.totp.login.client.model.DirectAuthenticationRequest;
import com.quakearts.auth.server.totp.services.DirectAuthenticationService;
import com.quakearts.rest.client.exception.HttpClientException;

public class TOTPDirectLoginModuleTest extends TOTPTestBase {

	public TOTPDirectLoginModuleTest() {
		System.setProperty("authentication.fallback.timeout","1000");
		System.setProperty("fallback.token.broker.max.size", "1");
	}
	
	//all ok otp
	@Test
	public void testAllOkAuthenticate() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			for(Callback callback:callbacks) {
				if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-ok");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>();
		
		options.put("totp.server.url", "http://localhost:8080/totp");
		options.put("application.name", "TestApplication");
		
		TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
		loginModule.initialize(subject, callbackHandler, new HashMap<>(), options);
		assertThat(loginModule.login(),is(true));
		assertThat(loginModule.commit(),is(true));
		assertThat(loginModule.logout(),is(true));
		assertThat(loginModule.abort(),is(true));
		
		assertThat(subject.getPrincipals(TOTPDevicePrincipal.class).size(), is(1));
		assertThat(subject.getPrincipals(TOTPDevicePrincipal.class).iterator().next(), 
					is(new TOTPDevicePrincipal("TOTP-Authenticated")));
		assertThat(subject.getPrincipals(RolesGroup.class).size(), is(1));
		assertThat(subject.getPrincipals(RolesGroup.class).iterator().next()
				.members().nextElement(), is(new TOTPDevicePrincipal("TOTP-Authenticated")));

		subject = new Subject();
		RolesGroup rolesGroup = new RolesGroup("Roles");
		Group otherGroup = getOtherGroup();
		subject.getPrincipals().add(otherGroup);
		subject.getPrincipals().add(()->"Anonymous");
		subject.getPrincipals().add(rolesGroup);
		loginModule.initialize(subject, callbackHandler, new HashMap<>(), options);
		assertThat(loginModule.login(),is(true));
		assertThat(loginModule.commit(),is(true));
		assertThat(rolesGroup.members().nextElement(), is(new TOTPDevicePrincipal("TOTP-Authenticated")));
	}

	@Test
	public void testAllOkAuthenticateWithFallback() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			for(Callback callback:callbacks) {
				if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-ok-fallback");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
		
		options.put("allow.fallback", "true");
		
		TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
		
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		CompletableFuture.runAsync(()->{
			DirectAuthenticationService.getInstance()
			.putFallbackToken("testdevice-ok-fallback", "123456");
		});
		assertThat(loginModule.login(),is(true));
		assertThat(loginModule.commit(),is(true));
		assertThat(loginModule.logout(),is(true));
		assertThat(loginModule.abort(),is(true));
		
		assertThat(subject.getPrincipals(TOTPDevicePrincipal.class).size(), is(1));
		assertThat(subject.getPrincipals(TOTPDevicePrincipal.class).iterator().next(), 
					is(new TOTPDevicePrincipal("TOTP-Authenticated")));
		assertThat(subject.getPrincipals(RolesGroup.class).size(), is(1));
		assertThat(subject.getPrincipals(RolesGroup.class).iterator().next()
				.members().nextElement(), is(new TOTPDevicePrincipal("TOTP-Authenticated")));
		
		await().atLeast(Duration.ONE_SECOND).untilAsserted(()->{
			assertThat(DirectAuthenticationService.getInstance()
					.getFallbackToken("testdevice-ok-fallback"), is("XXXXXX"));
		});
	}	

	@Test
	public void testAllOkAuthenticateWithFallbackWithListener() throws Exception {
		try {
			DirectAuthenticationService.getInstance()
				.setFallbackListener("testdevice-ok-fallback-2", ()->{
				DirectAuthenticationService.getInstance()
				.putFallbackToken("testdevice-ok-fallback-2", "123456");
			});
			Subject subject = new Subject();
			CallbackHandler callbackHandler = callbacks->{
				for(Callback callback:callbacks) {
					if(callback instanceof NameCallback) {
						((NameCallback)callback).setName("testdevice-ok-fallback-2");
					}
				}
			};
			
			Map<String, Object> options = new HashMap<>(),
					sharedstate = new HashMap<>();
			
			options.put("allow.fallback", "true");
			
			TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
			
			loginModule.initialize(subject, callbackHandler, sharedstate, options);
			assertThat(loginModule.login(),is(true));
			assertThat(loginModule.commit(),is(true));
			assertThat(loginModule.logout(),is(true));
			assertThat(loginModule.abort(),is(true));
			assertThat(subject.getPrincipals(TOTPDevicePrincipal.class).size(), is(1));
			assertThat(subject.getPrincipals(TOTPDevicePrincipal.class).iterator().next(), 
						is(new TOTPDevicePrincipal("TOTP-Authenticated")));
			assertThat(subject.getPrincipals(RolesGroup.class).size(), is(1));
			assertThat(subject.getPrincipals(RolesGroup.class).iterator().next()
					.members().nextElement(), is(new TOTPDevicePrincipal("TOTP-Authenticated")));
		} finally {
			DirectAuthenticationService.getInstance()
				.setFallbackListener("testdevice-ok-fallback-2", null);
		}
		assertThat(DirectAuthenticationService.getInstance()
				.getFallbackToken("testdevice-ok-fallback-2"), is("XXXXXX"));
	}
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testAuthenticateWithoutFallback() throws Exception {
		expectedException.expect(LoginException.class);
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			for(Callback callback:callbacks) {
				if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-ok-fallback");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
		
		options.put("", "true");
		
		TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
		
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}
	
	@Test
	public void testNoFailOnError() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			for(Callback callback:callbacks) {
				if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-ok-fallback");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
		
		options.put("fail.on.error", "false");
		
		TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
		
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		assertThat(loginModule.login(),is(false));
		assertThat(loginModule.commit(),is(false));
	}
		
	@Test
	public void testAuthenticateWithNon404Error() throws Exception {
		expectedException.expect(LoginException.class);
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			for(Callback callback:callbacks) {
				if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-locked");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
		
		TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
		
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}
	
	@Test
	public void testAuthenticateWithNoCallbacks() throws Exception {
		expectedException.expect(LoginException.class);
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
		
		TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
		
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}
	
	//callbackhandler with IOException
	@Test
	public void testCallbackhandlerIOException() throws Exception {
		expectedException.expect(LoginException.class);
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			throw new IOException("Error thrown");
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}
	
	//callbackhandler with UnsupportedCallbackException	
	@Test
	public void testCallbackhandlerUnsupportedCallbackException() throws Exception {
		expectedException.expect(LoginException.class);
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			throw new UnsupportedCallbackException(callbacks[0]);
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}
	
	//Connector error 404
	@Test
	public void testConnectorError404() throws Exception {
		expectedException.expect(LoginException.class);
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			for(Callback callback:callbacks) {
				if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-not-found");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}
	
	//IOException
	@Test
	public void testIOException() throws Exception {
		expectedException.expect(LoginException.class);
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			for(Callback callback:callbacks) {
				if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-not-found");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
		
		options.put("totp.server.url", "http://localhost:8000/totp");
		
		TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}
	
	@Test
	public void testJSONException() throws Exception {
		expectedException.expect(LoginException.class);
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			for(Callback callback:callbacks) {
				if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-deserialize-error");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
				
		TOTPDirectLoginModule loginModule = new TOTPDirectLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}
	
	@Test
	public void testTOTPClientHTTPSerializationError() throws Exception {
		expectedException.expect(HttpClientException.class);
		expectedException.expectMessage("Unable to serialize object");
		TOTPHttpClient client = TOTPHttpClientBuilder
				.createTOTPServerHttpClient("http://localhost:8080/totp");
		
		client.authenticateDirectly(new DirectAuthenticationRequestBomb());
	}
	
	class DirectAuthenticationRequestBomb 
		extends DirectAuthenticationRequest {
		@Override
		public String getDeviceId() {
			throw new UnsupportedOperationException();
		}
	}
}
