package com.quakearts.auth.server.totp.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AuthenticationServiceImplTest.class, DeviceServiceImplTest.class, KeyGeneratorImplTest.class,
		LoginModuleServiceTest.class, RESTServiceTest.class, TOTPGeneratorImplTest.class, TOTPLoginModuleTest.class })
public class AllTOTPTests {

}
