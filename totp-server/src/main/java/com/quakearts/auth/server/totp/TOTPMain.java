package com.quakearts.auth.server.totp;

import com.quakearts.appbase.Main;

public class TOTPMain {

	public static void main(String[] args) {
		Main.main(new String[] {InitMain.class.getName(),"-dontwaitinmain"});
	}

	public static void stop(String[] args) {
		System.exit(0);
	}	
}
