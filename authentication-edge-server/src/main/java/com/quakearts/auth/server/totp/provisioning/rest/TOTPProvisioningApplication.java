package com.quakearts.auth.server.totp.provisioning.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.quakearts.auth.server.totp.provisioning.client.exception.GeneralExceptionMapper;

@ApplicationPath("totp-provisioning")
public class TOTPProvisioningApplication extends Application {
	private Set<Class<?>> classes = new HashSet<>(Arrays.asList(AuthenticationResource.class,
			GeneralExceptionMapper.class));
	
	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}
}
