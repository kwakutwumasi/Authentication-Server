package com.quakearts.auth.server.proxy.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.quakearts.auth.server.proxy.client.exception.ConnectorExceptionMapper;
import com.quakearts.auth.server.proxy.client.exception.GeneralExceptionMapper;
import com.quakearts.auth.server.proxy.client.exception.HttpClientExceptionMapper;
import com.quakearts.auth.server.proxy.client.exception.IOExceptionMapper;

@ApplicationPath("/")
public class AuthenticationEdgeApplication extends Application {
	private Set<Class<?>> classes = new HashSet<>(Arrays.asList(
			AuthenticationResource.class,
			GeneralExceptionMapper.class,
			ConnectorExceptionMapper.class,
			HttpClientExceptionMapper.class,
			IOExceptionMapper.class));
	
	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}
}
