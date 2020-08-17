package com.quakearts.auth.server.totp.login.testmain;

import com.quakearts.appbase.Main;

public class Startup {
	public static void main(String[] args) {
		System.setProperty("authentication.fallback.timeout", "30000");
		Main.main(new String[]{AppInit.class.getName(),"-dontwaitinmain"});
	}
}
