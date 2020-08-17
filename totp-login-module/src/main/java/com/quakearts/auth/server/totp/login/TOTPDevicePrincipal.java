package com.quakearts.auth.server.totp.login;

import java.security.Principal;
import java.util.Objects;

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
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TOTPDevicePrincipal other = (TOTPDevicePrincipal) obj;
		return Objects.equals(name, other.name);
	}

}
