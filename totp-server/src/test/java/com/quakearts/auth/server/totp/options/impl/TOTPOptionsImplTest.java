package com.quakearts.auth.server.totp.options.impl;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import javax.enterprise.inject.spi.CDI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.appbase.Main;
import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.appbase.internal.properties.ConfigurationPropertyMap;
import com.quakearts.auth.server.totp.alternatives.AlternativeTOTPConfigurationProvider;
import com.quakearts.auth.server.totp.alternatives.AlternativeTOTPOptions;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.webtools.test.CDIRunner;

@RunWith(CDIRunner.class)
public class TOTPOptionsImplTest {
	
	@Test
	public void testAllFieldsPresent() throws Exception {
		TOTPOptionsImpl totpOptionsImpl = new TOTPOptionsImpl();
		AlternativeTOTPConfigurationProvider provider = 
				new AlternativeTOTPConfigurationProvider(getAllOptions(), "test");
		Field totpConfigurationProvider = TOTPOptionsImpl.class.getDeclaredField("totpConfigurationProvider");
		totpConfigurationProvider.setAccessible(true);
		totpConfigurationProvider.set(totpOptionsImpl, provider);		
		totpOptionsImpl.init();
		assertThat(totpOptionsImpl.getDeviceConnectionThreads(), is(5));
		assertThat(totpOptionsImpl.getDeviceConnectionPerformancePreferences(), is(notNullValue()));
		assertThat(totpOptionsImpl.getDeviceConnectionPerformancePreferences().getConnectionTime(), is(1));
		assertThat(totpOptionsImpl.getDeviceConnectionPerformancePreferences().getBandwidth(), is(1024));
		assertThat(totpOptionsImpl.getDeviceConnectionPerformancePreferences().getLatency(), is(1));
		assertThat(totpOptionsImpl.getDeviceConnectionReceiveBufferSize(), is(1024));
		assertThat(totpOptionsImpl.getDeviceConnectionReuseAddress(), is(Boolean.TRUE));
		assertThat(totpOptionsImpl.getDeviceConnectionEchoInterval(), is(30000L));
		assertThat(totpOptionsImpl.getServerJwtConfigName(), is("login2.config"));
		assertThat(totpOptionsImpl.getRequestSigningJwtConfigName(), is("login3.config"));
		assertThat(totpOptionsImpl.getAllowedOrigins(), is("http://localhost:8080;http://localhost:8081"));
		assertThat(totpOptionsImpl.getDeviceConnectionKeyPassword(), is("keypassword"));
		assertThat(totpOptionsImpl.isInEnhancedMode(), is(false));
	}
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void testInstalledAdministratorsMissing() throws Exception {
		exception.expect(ConfigurationException.class);
		exception.expectMessage("Entry 'installed.administrators' is missing from test and is required");
		TOTPOptionsImpl totpOptionsImpl = new TOTPOptionsImpl();
		ConfigurationPropertyMap configurationPropertyMap = getAllOptions();
		configurationPropertyMap.remove("installed.administrators");
		AlternativeTOTPConfigurationProvider provider = 
				new AlternativeTOTPConfigurationProvider(configurationPropertyMap, "test");
		Field totpConfigurationProvider = TOTPOptionsImpl.class.getDeclaredField("totpConfigurationProvider");
		totpConfigurationProvider.setAccessible(true);
		totpConfigurationProvider.set(totpOptionsImpl, provider);		
		totpOptionsImpl.init();
	}

	@Test
	public void testDeviceConnectionMissing() throws Exception {
		exception.expect(ConfigurationException.class);
		exception.expectMessage("Entry 'device.connection' is missing from test and is required");
		TOTPOptionsImpl totpOptionsImpl = new TOTPOptionsImpl();
		ConfigurationPropertyMap configurationPropertyMap = getAllOptions();
		configurationPropertyMap.remove("device.connection");
		AlternativeTOTPConfigurationProvider provider = 
				new AlternativeTOTPConfigurationProvider(configurationPropertyMap, "test");
		Field totpConfigurationProvider = TOTPOptionsImpl.class.getDeclaredField("totpConfigurationProvider");
		totpConfigurationProvider.setAccessible(true);
		totpConfigurationProvider.set(totpOptionsImpl, provider);		
		totpOptionsImpl.init();
	}

	@Test
	public void testDefaultModeIsInEnhancedModeTrue() throws Exception {
		AlternativeTOTPOptions.returnInEnhancedMode(null);
		TOTPOptions totpOptions = CDI.current().select(TOTPOptions.class).get();
		assertThat(totpOptions.isInEnhancedMode(), is(true));
	}
	
	private ConfigurationPropertyMap getAllOptions() throws IOException {
		return Main.getAppBasePropertiesLoader()
				.loadParametersFromReader("totpoptions.all.json", new InputStreamReader(Thread
						.currentThread().getContextClassLoader()
						.getResourceAsStream("totpoptions.all.json")));
	}
	
}
