package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.auth.server.totp.options.impl.TOTPConfigurationProviderImpl;

public class TOTPConfigurationProviderImplTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testGetConfigurationPropertyMapWithException() {
		exception.expect(ConfigurationException.class);
		exception.expectMessage("java.io.IOException");
		ClassLoader classLoader = new ClassLoader() {
			@Override
			public InputStream getResourceAsStream(String resName) {
				return new InputStream() {
					
					@Override
					public int read() throws IOException {
						throw new IOException();
					}
				};
			}
		};
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		try {
			TOTPConfigurationProviderImpl impl = new TOTPConfigurationProviderImpl();
			impl.getConfigurationPropertyMap();
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	@Test
	public void testGetConfigurationPropertyMapName() {
		assertThat(new TOTPConfigurationProviderImpl().getConfigurationPropertyMapName(), is("totpoptions.json"));
	}

}
