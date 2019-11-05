package com.quakearts.auth.server.totp.test.main;

import com.quakearts.appbase.Main;
import com.quakearts.auth.server.totp.InitMain;

public class TestMain {

    public static void main(String[] args){
		Main.main(new String[] {InitMain.class.getName(),"-dontwaitinmain"});
	}

}
