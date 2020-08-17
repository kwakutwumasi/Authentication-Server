package com.quakearts.auth.server.totp.login.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

import java.io.IOException;
import java.security.acl.Group;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.quakearts.auth.server.totp.login.RolesGroup;
import com.quakearts.auth.server.totp.login.TOTPDevicePrincipal;
import com.quakearts.auth.server.totp.login.TOTPLoginModule;
import com.quakearts.auth.server.totp.login.client.TOTPHttpClient;
import com.quakearts.auth.server.totp.login.client.TOTPHttpClientBuilder;
import com.quakearts.auth.server.totp.login.client.model.AuthenticationRequest;
import com.quakearts.rest.client.exception.HttpClientException;

public class TOTPLoginModuleTest extends TOTPTestBase {

	//all ok otp
	@Test
	public void testAllOkAuthenticate() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			for(Callback callback:callbacks) {
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("password123456".toCharArray());
				} else if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-ok");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
		
		options.put("totp.server.url", "http://localhost:8080/totp");
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
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

		TOTPDevicePrincipal devicePrincipal = new TOTPDevicePrincipal("testdevice-ok");
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
		assertThat(sharedstate.get("javax.security.auth.login.password"), is("password".toCharArray()));

		subject = new Subject();
		RolesGroup rolesGroup = new RolesGroup("Roles");
		Group otherGroup = getOtherGroup();
		subject.getPrincipals().add(otherGroup);
		subject.getPrincipals().add(()->"Anonymous");
		subject.getPrincipals().add(rolesGroup);
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		assertThat(loginModule.login(),is(true));
		assertThat(loginModule.commit(),is(true));
		assertThat(rolesGroup.members().nextElement(), is(new TOTPDevicePrincipal("TOTP-Authenticated")));
	}

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testCallbackhandlerIOException() throws Exception {
		expectedException.expect(LoginException.class);
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			throw new IOException("Error thrown");
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}
	
	//callbackhandler with IOException
	@Test
	public void testNoFailOnError() throws Exception {
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			throw new IOException("Error thrown");
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		options.put("fail.on.error", "false");
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		assertThat(loginModule.login(),is(false));
		assertThat(loginModule.commit(),is(false));
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
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}
	
	//callback with default name
	//Connector error 403
	@Test
	public void testCallbackhandlerWithDefaultName() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage("Error-uknown");
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			for(Callback callback:callbacks) {
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("password123456".toCharArray());
				} 
			}	
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}
	
	//password null
	@Test
	public void testEmptyCallbackhandler() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage("Username/Password is null.");
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		loginModule.initialize(subject, callbackHandler, sharedstate, options);
		try {
			assertThat(loginModule.login(),is(false));
		} finally {
			assertThat(loginModule.commit(),is(false));
		}
	}

	//password too short
	@Test
	public void testShortPassword() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage("Username/Password is null.");
		Subject subject = new Subject();
		CallbackHandler callbackHandler = callbacks->{
			for(Callback callback:callbacks) {
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("pass".toCharArray());
				} else if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-ok");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
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
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("password123456".toCharArray());
				} else if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-not-found");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = null;
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
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
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("password123456".toCharArray());
				} else if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-not-found");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
		
		options.put("totp.server.url", "http://localhost:8000/totp");
		
		TOTPLoginModule loginModule = new TOTPLoginModule();
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
				if(callback instanceof PasswordCallback) {
					((PasswordCallback)callback).setPassword("password123456".toCharArray());
				} else if(callback instanceof NameCallback) {
					((NameCallback)callback).setName("testdevice-deserialize-error");
				}
			}
		};
		
		Map<String, Object> options = new HashMap<>(),
				sharedstate = new HashMap<>();
				
		TOTPLoginModule loginModule = new TOTPLoginModule();
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
		
		client.authenticate(new AuthenticationRequestBomb());
	}
	
	@Test
	public void testRolesGroup() throws Exception {
		RolesGroup rolesGroup = new RolesGroup("Test");
		assertThat(rolesGroup.getName(), is("Test"));
		assertThat(rolesGroup.addMember(new TOTPDevicePrincipal("TestPrincipal")), is(true));
		assertThat(rolesGroup.isMember(new TOTPDevicePrincipal("TestPrincipal")), is(true));
		assertThat(rolesGroup.members().nextElement(), is(new TOTPDevicePrincipal("TestPrincipal")));
		assertThat(rolesGroup.removeMember(new TOTPDevicePrincipal("TestPrincipal")), is(true));
		assertThat(rolesGroup.isMember(new TOTPDevicePrincipal("TestPrincipal")), is(false));
	}
	
	class AuthenticationRequestBomb 
		extends AuthenticationRequest {
		@Override
		public String getDeviceId() {
			throw new UnsupportedOperationException();
		}
	}
}
