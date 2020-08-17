package com.quakearts.auth.server.totp.login.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TOTPDirectLoginModuleTest.class, TOTPLoginModuleTest.class })
public class AllTOTPLoginModuleTests {}
