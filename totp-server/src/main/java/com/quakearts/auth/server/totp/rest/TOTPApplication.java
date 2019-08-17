package com.quakearts.auth.server.totp.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.quakearts.auth.server.totp.exception.DataStoreExceptionMapper;
import com.quakearts.auth.server.totp.exception.GeneralExceptionMapper;
import com.quakearts.auth.server.totp.exception.RestSecurityExceptionMapper;
import com.quakearts.auth.server.totp.exception.TOTPExceptionMapper;

@ApplicationPath("totp")
public class TOTPApplication extends Application {
	private Set<Class<?>> classes = new HashSet<>(
			Arrays.asList(AuthenticationResource.class,
					ManagementLoginResource.class,
					ManagementResource.class,
					ProvisioningResource.class,
					SynchronizeResource.class,
					DataStoreExceptionMapper.class,
					GeneralExceptionMapper.class,
					RestSecurityExceptionMapper.class,
					TOTPExceptionMapper.class));

	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}
}
