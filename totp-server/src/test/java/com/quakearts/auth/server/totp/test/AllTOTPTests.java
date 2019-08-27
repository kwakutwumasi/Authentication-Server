package com.quakearts.auth.server.totp.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.quakearts.auth.server.totp.options.impl.TOTPOptionsImplTest;

@RunWith(Suite.class)
@SuiteClasses({ AuthenticationResourceTest.class, 
		AuthenticationServiceImplTest.class, ConnectionManagerImplTest.class, 
		DeviceAuthorizationServiceTest.class,
		DeviceConnectionChannelImplTest.class,
		DeviceServiceImplTest.class, KeyGeneratorImplTest.class,
		ModelTests.class, RESTServiceTest.class, TOTPConfigurationProviderImplTest.class, 
		TOTPGeneratorImplTest.class, TOTPOptionsImplTest.class})
public class AllTOTPTests {

}
