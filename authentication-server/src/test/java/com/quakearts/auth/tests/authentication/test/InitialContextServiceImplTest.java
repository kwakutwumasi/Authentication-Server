package com.quakearts.auth.tests.authentication.test;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import javax.inject.Inject;
import javax.naming.NamingException;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.rest.services.impl.InitialContextServiceImpl;

@RunWith(MainRunner.class)
public class InitialContextServiceImplTest {

	@Inject
	private InitialContextServiceImpl serviceImpl;
	
	@Test
	public void testAll() throws Exception {
		serviceImpl.createContext("java:/test");
		serviceImpl.bind("java:/test/Test", "Test");
		assertThat(serviceImpl.lookup("java:/test/Test"), is("Test"));
		serviceImpl.unbind("java:/test/Test");
		
		try {
			serviceImpl.lookup("java:/test/Test");
			fail("Naming exception was not thrown");
		} catch (NamingException e) {
		}
	}

}
