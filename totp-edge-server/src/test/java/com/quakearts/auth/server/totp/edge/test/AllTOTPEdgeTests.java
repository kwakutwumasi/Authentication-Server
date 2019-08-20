package com.quakearts.auth.server.totp.edge.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ DeviceConnectionEndpointTest.class, DeviceConnectionServiceImplTest.class, RESTServicesTest.class,
		TOTPServerConnectionImplTest.class, TOTPServerHttpClientTest.class, TOTPServerMessageHandlerImplTest.class })
public class AllTOTPEdgeTests {

}
