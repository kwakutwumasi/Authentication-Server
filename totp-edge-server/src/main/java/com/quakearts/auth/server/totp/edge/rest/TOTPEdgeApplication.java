package com.quakearts.auth.server.totp.edge.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.quakearts.auth.server.totp.edge.exception.GeneralExceptionMapper;

@ApplicationPath("totp-provisioning")
public class TOTPEdgeApplication extends Application {
	private Set<Class<?>> classes = new HashSet<>(Arrays.asList(ProvisioningResource.class,
			AuthenticationResource.class,
			SynchronizeResource.class,
			GeneralExceptionMapper.class));
	
	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}
}
