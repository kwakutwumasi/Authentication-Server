package com.quakearts.auth.server.totp.edge.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.quakearts.auth.server.totp.edge.exception.ConnectorExceptionMapper;
import com.quakearts.auth.server.totp.edge.exception.GeneralExceptionMapper;
import com.quakearts.auth.server.totp.edge.exception.HttpClientExceptionMapper;
import com.quakearts.auth.server.totp.edge.exception.IOExceptionMapper;

@ApplicationPath("totp-provisioning")
public class TOTPEdgeApplication extends Application {
	@Override
	public Set<Class<?>> getClasses() {
		return new HashSet<>(Arrays.asList(
				ProvisioningResource.class,
				AuthenticationResource.class,
				SynchronizeResource.class,
				ConnectorExceptionMapper.class,
				HttpClientExceptionMapper.class,
				IOExceptionMapper.class,
				GeneralExceptionMapper.class));
	}
}
