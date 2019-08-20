package com.quakearts.auth.server.totp.edge.channel;

import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

public interface DeviceConnectionService {
	void registerConnection(DeviceConnection connection);
	void unregisterConnection(DeviceConnection connection);
	Payload send(Payload payload) throws UnconnectedDeviceException;
}
