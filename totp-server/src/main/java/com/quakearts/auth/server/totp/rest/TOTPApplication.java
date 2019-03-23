package com.quakearts.auth.server.totp.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.quakearts.auth.server.totp.exception.DataStoreExceptionMapper;
import com.quakearts.auth.server.totp.exception.GeneralExceptionMapper;
import com.quakearts.auth.server.totp.exception.RestSecurityExceptionMapper;

@ApplicationPath("totp")
public class TOTPApplication extends Application {
	private Set<Class<?>> classes = new HashSet<>(
			Arrays.asList(LoginResource.class,
					ManagementResource.class,
					ProvisioningResource.class,
					SynchronizeResource.class,
					DataStoreExceptionMapper.class,
					GeneralExceptionMapper.class,
					RestSecurityExceptionMapper.class,
					ProvisioningResource.class));

	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}
}
