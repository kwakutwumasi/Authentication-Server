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
		DeviceRequestSigningServiceImplTest.class,
		DeviceManagementServiceImplTest.class,
		KeyGeneratorImplTest.class,
		ManagementResourceTest.class,
		ModelTests.class,
		RESTServiceTest.class, 
		RequestSigningResourceTest.class,
		TOTPConfigurationProviderImplTest.class, 
		TOTPGeneratorImplTest.class, 
		TOTPOptionsImplTest.class})
public class AllTOTPTests {

}
