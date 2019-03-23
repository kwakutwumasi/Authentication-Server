package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

import java.io.FileNotFoundException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeAuthenticationService;
import com.quakearts.auth.server.totp.alternatives.AlternativeDeviceService;
import com.quakearts.auth.server.totp.loginmodule.TOTPLoginModule;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.webtools.test.AllServicesRunner;
import com.quakearts.webapp.security.auth.DirectoryRoles;
import com.quakearts.webapp.security.auth.OtherPrincipal;
import com.quakearts.webapp.security.auth.UserPrincipal;

@RunWith(AllServicesRunner.class)
public class TOTPLoginModuleTest {

	@Test
	public void testLoginTrack1() throws Exception {
		TOTPLoginModule loginModule = new TOTPLoginModule();
		Subject subject = new Subject();
		
		Map<String, Object> sharedState = new HashMap<>();
		Map<String, Object> options = new HashMap<>();
		options.put(TOTPLoginModule.USE_FIRST_PASS, "true");
		options.put(TOTPLoginModule.TOTP_ROLENAME, "TestRoles");
		options.put(TOTPLoginModule.TOTP_DEFAULTROLES, "Role1;Role2;Role3");
		
		sharedState.put("javax.security.auth.login.name", new UserPrincipal("testlogin1"));
		sharedState.put("javax.security.auth.login.password", "123456".toCharArray());
		
		prepareLoginOk();
		
		loginModule.initialize(subject, callbacks->{
			
		}, sharedState, options);
		
		assertThat(loginModule.login(), is(true));
		assertThat(loginModule.commit(), is(true));
		
		assertThat(subject.getPrincipals().size(), is(5));
		List<String> rolesValues = Arrays.asList("Role1","Role2","Role3");
		for(OtherPrincipal principal: subject.getPrincipals(OtherPrincipal.class)) {
			assertThat(rolesValues.contains(principal.getName()), is(true));
		}
		
		for(DirectoryRoles roles: subject.getPrincipals(DirectoryRoles.class)) {
			assertThat(roles.getName(), is("TestRoles"));
			Enumeration<? extends Principal> principals = roles.members();
			int count=0;
			while (principals.hasMoreElements()) {
				++count;
				Principal principal = principals.nextElement();
				if(principal instanceof UserPrincipal) {
					assertThat(principal.getName(), is("testlogin1"));		
				} else {
					assertThat(rolesValues.contains(principal.getName()), is(true));		
				}
			}
			assertThat(count, is(4));
		}
		
		assertThat(loginModule.logout(), is(true));
		assertThat(loginModule.abort(), is(true));
	}

	private void prepareLoginOk() {
		Device device = new Device();
		device.setStatus(Status.ACTIVE);

		AlternativeDeviceService.returnDevice(id-> {
			assertThat(id, is("testlogin1"));
			return Optional.of(device);
		});
		
		AlternativeAuthenticationService.returnAuthenticate((authDevice,otp)->{
					assertThat(authDevice, is(device));
					assertThat(otp, is("123456"));
					return true;
				});
		
		AlternativeAuthenticationService.returnLocked(checkDevice->{
			assertThat(checkDevice, is(device));
			return false;
		});
	}
	
	@Test
	public void testLoginTrack2() throws Exception {
		TOTPLoginModule loginModule = new TOTPLoginModule();
		Subject subject = new Subject();
		
		OtherPrincipal otherPrincipal = new OtherPrincipal("existing");
		subject.getPrincipals().add(otherPrincipal);
		
		Map<String, Object> sharedState = new HashMap<>();
		Map<String, Object> options = new HashMap<>();
		options.put(TOTPLoginModule.USE_FIRST_PASS, "true");
		
		sharedState.put("javax.security.auth.login.name", new UserPrincipal("testlogin1"));
		
		prepareLoginOk();
		
		loginModule.initialize(subject, callbacks->{
			for(Callback callback: callbacks){
				if(callback instanceof NameCallback){
					((NameCallback)callback).setName("testlogin1");
				}
				if(callback instanceof PasswordCallback){
					((PasswordCallback)callback).setPassword("123456".toCharArray());;
				}
			}
		}, sharedState, options);
		
		assertThat(loginModule.login(), is(true));
		assertThat(loginModule.commit(), is(true));
		
		assertThat(subject.getPrincipals().size(), is(3));
		List<String> rolesValues = Arrays.asList("existing");
		
		for(OtherPrincipal principal: subject.getPrincipals(OtherPrincipal.class)) {
			assertThat(rolesValues.contains(principal.getName()), is(true));
		}
		
		for(DirectoryRoles roles: subject.getPrincipals(DirectoryRoles.class)) {
			assertThat(roles.getName(), is("Roles"));
			Enumeration<? extends Principal> principals = roles.members();
			int count=0;
			while (principals.hasMoreElements()) {
				++count;
				Principal principal = principals.nextElement();
				if(principal instanceof UserPrincipal) {
					assertThat(principal.getName(), is("testlogin1"));		
				} else {
					assertThat(rolesValues.contains(principal.getName()), is(true));		
				}
			}
			assertThat(count, is(1));
		}
		
		assertThat(loginModule.logout(), is(true));
		assertThat(loginModule.abort(), is(true));
	}
	
	@Test
	public void testLoginTrack3() throws Exception {
		TOTPLoginModule loginModule = new TOTPLoginModule();
		Subject subject = new Subject();
		
		DirectoryRoles roles = new DirectoryRoles("Roles");
		subject.getPrincipals().add(roles);
		roles.addMember(new OtherPrincipal("existing"));
		
		Map<String, Object> sharedState = new HashMap<>();
		Map<String, Object> options = new HashMap<>();
		options.put(TOTPLoginModule.USE_FIRST_PASS, "true");
		
		sharedState.put("javax.security.auth.login.name", "testlogin1");
		sharedState.put("javax.security.auth.login.password", "123456".toCharArray());
		
		prepareLoginOk();
		
		loginModule.initialize(subject, callbacks->{
			for(Callback callback: callbacks){
				if(callback instanceof NameCallback){
					((NameCallback)callback).setName("testlogin1");
				}
				if(callback instanceof PasswordCallback){
					((PasswordCallback)callback).setPassword("123456".toCharArray());;
				}
			}
		}, sharedState, options);
		
		assertThat(loginModule.login(), is(true));
		assertThat(loginModule.commit(), is(true));
		
		assertThat(subject.getPrincipals().size(), is(3));
		List<String> rolesValues = Arrays.asList("existing");
		
		for(OtherPrincipal principal: subject.getPrincipals(OtherPrincipal.class)) {
			assertThat(rolesValues.contains(principal.getName()), is(true));
		}
		
		for(DirectoryRoles gottenRoles: subject.getPrincipals(DirectoryRoles.class)) {
			assertThat(gottenRoles.getName(), is("Roles"));
			Enumeration<? extends Principal> principals = gottenRoles.members();
			int count=0;
			while (principals.hasMoreElements()) {
				++count;
				Principal principal = principals.nextElement();
				if(principal instanceof UserPrincipal) {
					assertThat(principal.getName(), is("testlogin1"));		
				} else {
					assertThat(rolesValues.contains(principal.getName()), is(true));		
				}
			}
			assertThat(count, is(2));
		}
		
		assertThat(loginModule.logout(), is(true));
		assertThat(loginModule.abort(), is(true));
	}
	
	@Test
	public void testLoginTrack4() throws Exception {
		TOTPLoginModule loginModule = new TOTPLoginModule();
		Subject subject = new Subject();
		
		DirectoryRoles roles = new DirectoryRoles("OtherRoles");
		subject.getPrincipals().add(roles);
		roles.addMember(new OtherPrincipal("existing"));
		
		Map<String, Object> sharedState = null;
		Map<String, Object> options = new HashMap<>();
		options.put(TOTPLoginModule.USE_FIRST_PASS, "true");
				
		prepareLoginOk();
		
		loginModule.initialize(subject, callbacks->{
			for(Callback callback: callbacks){
				if(callback instanceof NameCallback){
					((NameCallback)callback).setName("testlogin1");
				}
				if(callback instanceof PasswordCallback){
					((PasswordCallback)callback).setPassword("123456".toCharArray());;
				}
			}
		}, sharedState, options);
		
		assertThat(loginModule.login(), is(true));
		assertThat(loginModule.commit(), is(true));
		
		assertThat(subject.getPrincipals().size(), is(3));
		List<String> rolesValues = Arrays.asList("existing");
		
		for(OtherPrincipal principal: subject.getPrincipals(OtherPrincipal.class)) {
			assertThat(rolesValues.contains(principal.getName()), is(true));
		}
		
		for(DirectoryRoles gottenRoles: subject.getPrincipals(DirectoryRoles.class)) {
			if(gottenRoles.getName().equals("Roles")){
				Enumeration<? extends Principal> principals = gottenRoles.members();
				int count=0;
				while (principals.hasMoreElements()) {
					++count;
					Principal principal = principals.nextElement();
					assertThat(principal.getName(), is("testlogin1"));
				}
				assertThat(count, is(1));
			} else {
				assertThat(gottenRoles.getName(), is("OtherRoles"));
			}
		}
		
		assertThat(loginModule.logout(), is(true));
		assertThat(loginModule.abort(), is(true));
	}
	
	@Test
	public void testLoginTrack5() throws Exception {
		TOTPLoginModule loginModule = new TOTPLoginModule();
		Subject subject = new Subject();
		
		DirectoryRoles roles = new DirectoryRoles("OtherRoles");
		subject.getPrincipals().add(roles);
		roles.addMember(new OtherPrincipal("existing"));
		
		Map<String, Object> sharedState = null;
		Map<String, Object> options = new HashMap<>();
				
		prepareLoginOk();
		
		loginModule.initialize(subject, callbacks->{
			for(Callback callback: callbacks){
				if(callback instanceof NameCallback){
					((NameCallback)callback).setName("testlogin1");
				}
				if(callback instanceof PasswordCallback){
					((PasswordCallback)callback).setPassword("123456".toCharArray());;
				}
			}
		}, sharedState, options);
		
		assertThat(loginModule.login(), is(true));
		assertThat(loginModule.commit(), is(true));
		
		assertThat(subject.getPrincipals().size(), is(3));
		List<String> rolesValues = Arrays.asList("existing");
		
		for(OtherPrincipal principal: subject.getPrincipals(OtherPrincipal.class)) {
			assertThat(rolesValues.contains(principal.getName()), is(true));
		}
		
		for(DirectoryRoles gottenRoles: subject.getPrincipals(DirectoryRoles.class)) {
			if(gottenRoles.getName().equals("Roles")){
				Enumeration<? extends Principal> principals = gottenRoles.members();
				int count=0;
				while (principals.hasMoreElements()) {
					++count;
					Principal principal = principals.nextElement();
					assertThat(principal.getName(), is("testlogin1"));
				}
				assertThat(count, is(1));
			} else {
				assertThat(gottenRoles.getName(), is("OtherRoles"));
			}
		}
		
		assertThat(loginModule.logout(), is(true));
		assertThat(loginModule.abort(), is(true));
	}
	
	@Test
	public void testLoginTrack6() throws Exception {		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		Subject subject = new Subject();
		Map<String, Object> sharedState = null;
		Map<String, Object> options = new HashMap<>();
						
		loginModule.initialize(subject, callbacks->{
			throw new FileNotFoundException("Just cuz...");
		}, sharedState, options);
		loginModule.login();
		assertThat(loginModule.login(), is(false));
		assertThat(subject.getPrincipals().isEmpty(), is(true));
	}

	@Test
	public void testLoginTrack7() throws Exception {		
		TOTPLoginModule loginModule = new TOTPLoginModule();
		Subject subject = new Subject();
		Map<String, Object> sharedState = null;
		Map<String, Object> options = new HashMap<>();
						
		loginModule.initialize(subject, callbacks->{
			throw new UnsupportedCallbackException(callbacks[0], "Just cuz...");
		}, sharedState, options);
		loginModule.login();
		assertThat(loginModule.login(), is(false));
		assertThat(subject.getPrincipals().isEmpty(), is(true));
	}
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testLoginTrack8() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage(is("Username and password are required"));
		TOTPLoginModule loginModule = new TOTPLoginModule();
		Subject subject = new Subject();
		
		try {
			Map<String, Object> sharedState = null;
			Map<String, Object> options = new HashMap<>();
					
			loginModule.initialize(subject, callbacks->{
				
			}, sharedState, options);
			
			assertThat(loginModule.login(), is(true));
		} finally {
			assertThat(subject.getPrincipals().isEmpty(), is(true));
		}
	}
	
	@Test
	public void testLoginTrack9() throws Exception {
		expectedException.expect(LoginException.class);
		expectedException.expectMessage(is("Username and password are required"));
		TOTPLoginModule loginModule = new TOTPLoginModule();
		Subject subject = new Subject();
		
		try {
			Map<String, Object> sharedState = null;
			Map<String, Object> options = new HashMap<>();
					
			loginModule.initialize(subject, callbacks->{
				for(Callback callback: callbacks){
					if(callback instanceof PasswordCallback){
						((PasswordCallback)callback).setPassword("123456".toCharArray());;
					}
				}
			}, sharedState, options);
			
			assertThat(loginModule.login(), is(true));
		} finally {
			assertThat(subject.getPrincipals().isEmpty(), is(true));
			assertThat(loginModule.commit(), is(false));
		}
	}	
}
