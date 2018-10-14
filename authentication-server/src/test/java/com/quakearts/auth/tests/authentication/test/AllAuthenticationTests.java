package com.quakearts.auth.tests.authentication.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AuthenticationResourceTest.class, DataSourceResourceTest.class, DataSourceServiceImplTest.class,
		FileServiceImplTest.class, InitialContextServiceImplTest.class, LiveTests.class, MainTest.class,
		OptionsServiceImplTest.class, RegistrationResourceTest.class, SecretsResourceTest.class, ErrorServiceImplTest.class})
public class AllAuthenticationTests {}
