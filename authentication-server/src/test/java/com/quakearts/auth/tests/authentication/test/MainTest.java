package com.quakearts.auth.tests.authentication.test;

import static org.junit.Assert.*;

import javax.sql.DataSource;

import static org.hamcrest.core.IsNull.*;
import static org.hamcrest.core.Is.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.appbase.spi.factory.JavaNamingDirectorySpiFactory;
import com.quakearts.auth.server.Main;

@RunWith(MainRunner.class)
public class MainTest {

	@Test
	public void testMain() throws Exception {
		DataSource dataSource = (DataSource) JavaNamingDirectorySpiFactory
			.getInstance().getJavaNamingDirectorySpi()
			.getInitialContext().lookup("java:/jdbc/MainDS");
		assertThat(dataSource, is(notNullValue()));		
		assertThat(dataSource.getConnection(), is(notNullValue()));
		
		try {
			Main.getInstance().createDataSources("etc-empty-name");
			fail("Exception was not thrown");
		} catch (ConfigurationException e) {
		}
		
		try {
			Main.getInstance().createDataSources("etc-no-name");
			fail("Exception was not thrown");
		} catch (ConfigurationException e) {
		}
	}

}
