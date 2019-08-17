package com.quakearts.auth.server.totp.options.impl;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import javax.enterprise.inject.Alternative;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.quakearts.appbase.Main;
import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.appbase.internal.properties.ConfigurationPropertyMap;
import com.quakearts.auth.server.totp.alternatives.AlternativeTOTPConfigurationProvider;
import com.quakearts.auth.server.totp.options.impl.TOTPOptionsImpl;

@Alternative
public class TOTPOptionsImplTest extends TOTPOptionsImpl {
	
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
		assertThat(totpOptionsImpl.getDeviceConnectionEchoInterval(), is(0L));
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

	private ConfigurationPropertyMap getAllOptions() throws IOException {
		return Main.getAppBasePropertiesLoader()
				.loadParametersFromReader("totpoptions.all.json", new InputStreamReader(Thread
						.currentThread().getContextClassLoader()
						.getResourceAsStream("totpoptions.all.json")));
	}
	
}
