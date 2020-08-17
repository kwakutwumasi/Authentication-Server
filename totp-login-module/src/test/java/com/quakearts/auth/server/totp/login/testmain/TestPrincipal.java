package com.quakearts.auth.server.totp.login.testmain;

import java.security.Principal;

public class TestPrincipal implements Principal {

	@Override
	public String getName() {
		return "LoggedIn";
	}

}
