package com.quakearts.auth.tests.authentication.test;

import static org.junit.Assert.*;

import java.io.File;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsInstanceOf.*;
import static org.hamcrest.core.IsNull.*;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.appbase.spi.factory.JavaNamingDirectorySpiFactory;
import com.quakearts.auth.server.rest.services.impl.DataSourceServiceImpl;

@RunWith(MainRunner.class)
public class DataSourceServiceImplTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Inject
	private DataSourceServiceImpl serviceImpl;
	
	@Test
	public void testGetAllDataSources() {
		assertThat(serviceImpl.getAllDataSources().contains("java:/jdbc/MainDS"), is(true));
	}

	@Test
	public void testCreateUsingAndRemoveIdentifiedBy() throws Exception {
		File file = new File("src"+File.separator
				+"test"+File.separator+"resources"+File.separator+"test.create.ds.json");
		serviceImpl.createUsing(file);
	
		InitialContext context = JavaNamingDirectorySpiFactory
				.getInstance().getJavaNamingDirectorySpi()
				.getInitialContext();
		
		DataSource dataSource = (DataSource) context.lookup("java:/jdbc/MainDS2");
		assertThat(dataSource, is(notNullValue()));		
		assertThat(dataSource.getConnection(), is(notNullValue()));
		assertThat(serviceImpl.getAllDataSources().contains("java:/jdbc/MainDS2"), is(true));
			
		serviceImpl.removeIdentifiedBy(file);
		
		try {
			context.lookup("java:/jdbc/MainDS2");
			fail("Did not throw naming exception");
		} catch (NamingException e) {
		}
		assertThat(serviceImpl.getAllDataSources().contains("java:/jdbc/MainDS2"), is(false));
	}
	
	@Test
	public void testCreateUsingBindException() {
		expectedException.expect(ConfigurationException.class);
		expectedException.expectMessage(is("Unable to bind datasource TestDSBindError"));
		expectedException.expectCause(instanceOf(NamingException.class));
		
		serviceImpl.createUsing(new File("src"+File.separator
					+"test"+File.separator+"resources"+File.separator+"test.error.binding.ds.json"));
	}

	@Test
	public void testCreateUsingNoNamException() {
		expectedException.expect(ConfigurationException.class);
		expectedException.expectMessage(is("Missing parameter 'jndi.name' in default.ds.json"));
		
		serviceImpl.createUsing(new File("etc-no-name"+File.separator+"default.ds.json"));
	}

	@Test
	public void testCreateUsingEmptyNameException() {
		expectedException.expect(ConfigurationException.class);
		expectedException.expectMessage(is("Missing parameter 'jndi.name' in default.ds.json"));
		
		serviceImpl.createUsing(new File("etc-empty-name"+File.separator+"default.ds.json"));
	}

	@Test
	public void testRemoveIdentifiedByUnbindException() {
		expectedException.expect(ConfigurationException.class);
		expectedException.expectMessage(is("Unable to unbind datasource java:/jdbc/TestDSUnbindError"));
		expectedException.expectCause(instanceOf(NamingException.class));
		
		serviceImpl.removeIdentifiedBy(new File("src"+File.separator
					+"test"+File.separator+"resources"+File.separator
					+"test.error.unbinding.ds.json"));
	}

}
