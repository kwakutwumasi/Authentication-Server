package com.quakearts.auth.server.totp.login;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Iterator;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public abstract class TOTPLoginCommon implements LoginModule {

	protected Map<String, String> options;
	protected boolean loginOk;
	protected Subject subject;

	public boolean commit() throws LoginException {
		if(loginOk) {
			Principal principal = new TOTPDevicePrincipal("TOTP-Authenticated");
			subject.getPrincipals().add(principal);
			String rolesGroupName = options.getOrDefault("roles.group", "Roles");
			Group group = findRolesGroup(subject, rolesGroupName);
			if(group == null){
				group = new RolesGroup(rolesGroupName);
				subject.getPrincipals().add(group);
			}
			group.addMember(principal);
		}
		return loginOk;
	}

	protected Group findRolesGroup(Subject subject, String rolesGroupName) {
		Group rolesgrp = null;
		for (Iterator<Principal> i = subject.getPrincipals().iterator(); i.hasNext();) {
			Object obj = i.next();
			if (obj instanceof Group && ((Group) obj).getName().equals(rolesGroupName)) {
				rolesgrp = (Group) obj;
				break;
			}
		}
		return rolesgrp;
	}

}