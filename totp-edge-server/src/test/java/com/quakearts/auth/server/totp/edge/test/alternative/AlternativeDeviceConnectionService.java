package com.quakearts.auth.server.totp.edge.test.alternative;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnection;
import com.quakearts.auth.server.totp.edge.channel.DeviceConnectionService;
import com.quakearts.auth.server.totp.edge.channel.impl.DeviceConnectionServiceImpl;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

@Alternative
@Singleton
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
	public Payload send(Payload payload) throws UnconnectedDeviceException {
		if(returnPayload != null) {
			return returnPayload.apply(payload);
		}
		return wrapped.send(payload);
	}

	public static interface Function{
		Payload apply(Payload payload) throws UnconnectedDeviceException;
	}
}
