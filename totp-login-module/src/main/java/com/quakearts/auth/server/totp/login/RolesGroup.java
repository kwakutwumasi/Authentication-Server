package com.quakearts.auth.server.totp.login;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class RolesGroup implements Group {

	private String rolesGroupName;
	private Set<Principal> principals = new HashSet<>();

	public RolesGroup(String rolesGroupName) {
		this.rolesGroupName = rolesGroupName;
	}

	@Override
	public String getName() {
		return rolesGroupName;
	}

	@Override
	public boolean addMember(Principal user) {
		return principals.add(user);
	}

	@Override
	public boolean removeMember(Principal user) {
		return principals.remove(user);
	}

	@Override
	public boolean isMember(Principal member) {
		return principals.contains(member);
	}

	@Override
	public Enumeration<? extends Principal> members() {
		return Collections.enumeration(principals);
	}

}
