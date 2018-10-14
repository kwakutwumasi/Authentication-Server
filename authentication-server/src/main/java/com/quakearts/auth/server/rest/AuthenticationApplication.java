package com.quakearts.auth.server.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.quakearts.auth.server.exeptionmapper.ValidationExceptionMapper;
import com.quakearts.auth.server.rest.filter.AcceptContainerResponse;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

@ApplicationPath("/")
public class AuthenticationApplication extends Application {
	private Set<Class<?>> classes = 
			new HashSet<>(Arrays.asList(RegistrationResource.class, 
										AuthenticationResource.class,
										OpenApiResource.class,
										SecretsResource.class,
										DataSourcesResource.class,
										AcceptContainerResponse.class,
										ValidationExceptionMapper.class,
										OpenApiDefinition.class));
					
	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}
}
