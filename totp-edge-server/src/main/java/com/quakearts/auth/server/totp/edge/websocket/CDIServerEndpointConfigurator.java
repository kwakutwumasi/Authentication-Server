package com.quakearts.auth.server.totp.edge.websocket;

import javax.enterprise.inject.spi.CDI;

import org.apache.tomcat.websocket.server.DefaultServerEndpointConfigurator;

public class CDIServerEndpointConfigurator extends DefaultServerEndpointConfigurator {
	@Override
	public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
		return CDI.current().select(clazz).get();
	}
}
