package com.quakearts.auth.server.rest.services.impl;

import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.quakearts.appbase.spi.factory.JavaNamingDirectorySpiFactory;
import com.quakearts.auth.server.rest.services.InitialContextService;

@Singleton
public class InitialContextServiceImpl implements InitialContextService {
	
	private InitialContext getInitialContext() {
		return JavaNamingDirectorySpiFactory
				.getInstance().getJavaNamingDirectorySpi()
				.getInitialContext();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T lookup(String jndiName) throws NamingException {
		return (T) getInitialContext().lookup(jndiName);
	}

	@Override
	public void unbind(String jndiName) throws NamingException {
		getInitialContext().unbind(jndiName);
	}

	@Override
	public void bind(String jndiName, Object object) throws NamingException {
		getInitialContext().bind(jndiName, object);
	}
	
	@Override
	public void createContext(String contextName) throws NamingException {
		JavaNamingDirectorySpiFactory
		.getInstance().getJavaNamingDirectorySpi()
			.createContext(contextName);
	}
}
