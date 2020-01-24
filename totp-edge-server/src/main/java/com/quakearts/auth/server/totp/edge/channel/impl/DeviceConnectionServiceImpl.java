package com.quakearts.auth.server.totp.edge.channel.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.inject.Singleton;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnection;
import com.quakearts.auth.server.totp.edge.channel.DeviceConnectionService;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

@Singleton
public class DeviceConnectionServiceImpl implements DeviceConnectionService {

	private static final String DEVICE_ID = "deviceId";
	private Map<String, DeviceConnection> connections = new ConcurrentHashMap<>();
	
	@Override
	public void registerConnection(DeviceConnection connection) {
		connections.put(connection.getDeviceId(), connection);
	}

	@Override
	public void unregisterConnection(DeviceConnection connection) {
		connections.remove(connection.getDeviceId());
	}

	@Override
	public void send(Payload payload, Consumer<Payload> callback) throws UnconnectedDeviceException {
		if(!payload.getMessage().containsKey(DEVICE_ID)) {
			throw new UnconnectedDeviceException();
		}
		String deviceId = payload.getMessage().get(DEVICE_ID);
		if(connections.containsKey(deviceId)) {
			DeviceConnection connection = connections.get(deviceId);
			connection.send(payload, callback);
		} else {
			throw new UnconnectedDeviceException();
		}
	}

	@Override
	public boolean isConnected(String deviceId) {
		if(connections.containsKey(deviceId)){
			return connections.get(deviceId).isConnected();
		}
		return false;
	}
}
