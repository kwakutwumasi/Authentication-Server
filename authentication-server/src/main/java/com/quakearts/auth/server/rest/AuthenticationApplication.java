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
										SecretsResource.class,
										DataSourcesResource.class,
										AcceptContainerResponse.class,
										ValidationExceptionMapper.class,
										OpenApiDefinition.class));
					
	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}
	
	@Override
	public Set<Object> getSingletons() {
		OpenApiResource apiResource = new OpenApiResource();
		apiResource.setResourcePackages(new HashSet<>(Arrays.asList("com.quakearts.auth.server.rest")));
		return new HashSet<>(Arrays.asList(apiResource));
	}
}
