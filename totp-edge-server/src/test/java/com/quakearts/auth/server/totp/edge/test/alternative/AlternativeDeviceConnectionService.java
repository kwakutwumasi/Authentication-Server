package com.quakearts.auth.server.totp.edge.test.alternative;

import java.util.function.Consumer;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnection;
import com.quakearts.auth.server.totp.edge.channel.DeviceConnectionService;
import com.quakearts.auth.server.totp.edge.channel.impl.DeviceConnectionServiceImpl;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

@Alternative
@Singleton
@Priority(Interceptor.Priority.APPLICATION)
public class AlternativeDeviceConnectionService implements DeviceConnectionService {

	private static Function returnPayload;
	
	public static void returnPayload(Function newReturnPayload) {
		returnPayload = newReturnPayload;
	}
	
	@Inject
	private DeviceConnectionServiceImpl wrapped;
	
	@Override
	public void registerConnection(DeviceConnection connection) {
		wrapped.registerConnection(connection);
	}

	@Override
	public void unregisterConnection(DeviceConnection connection) {
		wrapped.unregisterConnection(connection);
	}

	@Override
	public void send(Payload payload, Consumer<Payload> callback) throws UnconnectedDeviceException {
		if(returnPayload != null) {
			callback.accept(returnPayload.apply(payload));
		} else {
			wrapped.send(payload, callback);
		}
	}

	public static interface Function {
		Payload apply(Payload payload) throws UnconnectedDeviceException;
	}

	@Override
	public boolean isConnected(String deviceId) {
		return wrapped.isConnected(deviceId);
	}
}
