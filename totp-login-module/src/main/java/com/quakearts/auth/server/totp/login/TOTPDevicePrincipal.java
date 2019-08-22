package com.quakearts.auth.server.totp.login;

import java.security.Principal;

public class TOTPDevicePrincipal implements Principal {

	private String name;

	public TOTPDevicePrincipal(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TOTPDevicePrincipal other = (TOTPDevicePrincipal) obj;
		if (name == null) {
			return other.name == null;
		} else {
			return name.equals(other.name);
		}
	}

}
