package com.quakearts.auth.server.totp.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.jboss.resteasy.plugins.interceptors.CorsFilter;

import com.quakearts.auth.server.totp.exception.DataStoreExceptionMapper;
import com.quakearts.auth.server.totp.exception.GeneralExceptionMapper;
import com.quakearts.auth.server.totp.exception.RestSecurityExceptionMapper;
import com.quakearts.auth.server.totp.exception.TOTPExceptionMapper;
import com.quakearts.auth.server.totp.options.TOTPOptions;

@ApplicationPath("totp")
public class TOTPApplication extends Application {
	private Set<Class<?>> classes = new HashSet<>(
			Arrays.asList(AliasResource.class,
					AuthenticationResource.class,
					ManagementLoginResource.class,
					ManagementResource.class,
					ProvisioningResource.class,
					RequestSigningResource.class,
					SynchronizeResource.class,
					DataStoreExceptionMapper.class,
					GeneralExceptionMapper.class,
					RestSecurityExceptionMapper.class,
					TOTPExceptionMapper.class));

	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}
	
	@Override
	public Set<Object> getSingletons() {
		TOTPOptions options = CDI.current().select(TOTPOptions.class).get();
		
		CorsFilter corsFilter = new CorsFilter();
		for(String origin : options.getAllowedOrigins().split(";")){
	        corsFilter.getAllowedOrigins().add(origin);
		}
		
        corsFilter.setAllowedMethods("OPTIONS, GET, POST, DELETE, PUT, PATCH");
        
        return new HashSet<>(Arrays.asList(corsFilter));
	}
}
