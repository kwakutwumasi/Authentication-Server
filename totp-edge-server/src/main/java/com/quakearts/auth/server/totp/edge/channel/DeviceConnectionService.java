package com.quakearts.auth.server.totp.edge.channel;

import java.util.function.Consumer;

import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

public interface DeviceConnectionService {
	void registerConnection(DeviceConnection connection);
	void unregisterConnection(DeviceConnection connection);
	void send(Payload payload, Consumer<Payload> callback) throws UnconnectedDeviceException;
	boolean isConnected(String deviceId);
}
