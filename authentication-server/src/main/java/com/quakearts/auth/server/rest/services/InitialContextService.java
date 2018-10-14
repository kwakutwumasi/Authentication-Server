package com.quakearts.auth.server.rest.services;

import javax.naming.NamingException;

public interface InitialContextService {
	<T> T lookup(String jndiName) throws NamingException;
	void bind(String jndiName, Object object) throws NamingException;
	void unbind(String jndiName) throws NamingException;
	void createContext(String contextName) throws NamingException;
}
