package com.quakearts.auth.server.totp.loginmodule;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.quakearts.webapp.security.auth.DirectoryRoles;
import com.quakearts.webapp.security.auth.LoginOperationException;
import com.quakearts.webapp.security.auth.OtherPrincipal;
import com.quakearts.webapp.security.auth.UserPrincipal;

public class TOTPLoginModule implements LoginModule {

	private Subject subject;
	private CallbackHandler callbackHandler;
	private Map<String, ?> sharedState;
	
	private String rolesgrpname;
	private boolean useFirstPass;
	private String username;
	private char[] passwordChars;
	private boolean loginOk;
	private String[] defaultroles;
	private Group rolesgrp;

	public static final String TOTP_ROLENAME = "totp.rolename";
	public static final String USE_FIRST_PASS = "use_first_pass";
	public static final String TOTP_DEFAULTROLES = "totp.defaultroles";
	private static final Logger log = Logger.getLogger(TOTPLoginModule.class.getName());
	private LoginModuleService moduleService;
	
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
		this.sharedState = sharedState;
		
		rolesgrpname = (String) options.get(TOTP_ROLENAME);
		if(rolesgrpname == null)
			rolesgrpname = "Roles";
			
		useFirstPass = Boolean.parseBoolean((String) options.get(USE_FIRST_PASS));
		
		moduleService = CDI.current().select(LoginModuleService.class).get();
		
		String defaultrolesStr = (String) options.get(TOTP_DEFAULTROLES);
		if(defaultrolesStr != null){
        	defaultroles = defaultrolesStr.split(";");
        	for(int i=0;i<defaultroles.length;i++)
        		defaultroles[i] = defaultroles[i].trim();
        }
	}

	@Override
	public boolean login() throws LoginException {
		if (useFirstPass && sharedState != null) {
			loadUsernameAndPassword();
		}
		
		try {
			if (!useFirstPass || username == null || passwordChars == null) {
				processCallbacks();
			}
			
			if(username == null || passwordChars == null){
				throw new LoginException("Username and password are required");
			}
			
			moduleService.login(username, passwordChars);
			performFinalActions();
			return loginOk;
		} catch (LoginException e) {
			log.log(Level.FINE, "Login processing failed", e);
			throw e;
		} catch (LoginOperationException e) {
			log.log(Level.SEVERE, "Login processing failed due to an internal error", e);
			return false;
		}		
	}

	private void loadUsernameAndPassword() {
		Object loginPrincipal = sharedState.get("javax.security.auth.login.name");
		Object passwordVal = sharedState.get("javax.security.auth.login.password");
		username = (loginPrincipal instanceof Principal) ? ((Principal) loginPrincipal).getName() : null;
		passwordChars = (passwordVal instanceof char[]) ? (char[]) passwordVal: null;
	}

	private void processCallbacks() throws LoginOperationException {
		NameCallback name = new NameCallback("Enter your username", "anonymous");
		PasswordCallback pass = new PasswordCallback("Enter your passwordChars:", false);
		Callback[] callbacks = new Callback[2];
		callbacks[0] = name;
		callbacks[1] = pass;

		try {
			callbackHandler.handle(callbacks);
		} catch (IOException | UnsupportedCallbackException e) {
			throw new LoginOperationException("Callback could not be processed", e);
		}

		username = name.getName() != null ? name.getName().trim():null;
		passwordChars = pass.getPassword();
	}

	@SuppressWarnings("unchecked")
	private void performFinalActions() {
		loginOk = true;
		if (sharedState != null) {
			UserPrincipal shareduser = new UserPrincipal(username);
			Map<String, Object> sharedStateObj = ((Map<String, Object>)sharedState);
			sharedStateObj.put("javax.security.auth.login.name", shareduser);
			sharedStateObj.put("javax.security.auth.login.password", passwordChars);
			sharedStateObj.put("com.quakearts.LoginOk", loginOk);					
		}
	}
	
	@Override
	public boolean commit() throws LoginException {
		if (loginOk) {
			Set<Principal> principalset = subject.getPrincipals();

			if (useFirstPass) {
				findRolesGroup(principalset);
			}
			createRolesGroup(principalset);
			loadDefaultRoles();
			addPrincipalSetToSubject(principalset);
			return true;
		} else {
			return false;
		}
	}
	
	private void findRolesGroup(Set<Principal> principalset) {
		for (Iterator<Principal> i = principalset.iterator(); i.hasNext();) {
			Object obj = i.next();
			if (obj instanceof Group && ((Group) obj).getName().equals(rolesgrpname)) {
				rolesgrp = (Group) obj;
				break;
			}
		}
	}
	
	private void loadDefaultRoles() {
		OtherPrincipal principal;
		if (defaultroles != null) {
			int count = 1;
			for (String role : defaultroles) {
				principal = new OtherPrincipal(role, "default"+(count++));
				rolesgrp.addMember(principal);
			}
		}
	}
	
	private void addPrincipalSetToSubject(Set<Principal> principalset) {
		rolesgrp.addMember(new UserPrincipal(username));
		Enumeration<? extends Principal> members = rolesgrp.members();
		while (members.hasMoreElements()) {
			Principal type = members.nextElement();
			principalset.add(type);				
		}
	}
	
	private void createRolesGroup(Set<Principal> principalset) {
		if (rolesgrp == null) {
			rolesgrp = new DirectoryRoles(rolesgrpname);
			principalset.add(rolesgrp);
		}
	}


	@Override
	public boolean abort() throws LoginException {
		defaultroles = null;
		rolesgrpname = null;
		subject = null;
		rolesgrp = null;
		callbackHandler = null;
		sharedState = null;
		loginOk = false;
		username = null;
		passwordChars = null;
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		return abort();
	}

}
