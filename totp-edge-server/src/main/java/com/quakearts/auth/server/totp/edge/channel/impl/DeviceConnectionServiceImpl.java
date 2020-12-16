package com.quakearts.auth.server.totp.edge.channel.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnection;
import com.quakearts.auth.server.totp.edge.channel.DeviceConnectionService;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

@Singleton
public class DeviceConnectionServiceImpl implements DeviceConnectionService {

	private static final Logger log = LoggerFactory.getLogger(DeviceConnectionService.class);
	
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
		log.debug("Sending message: {} to device with id hashCode: {}", payload,
				deviceId.hashCode());
		if(connections.containsKey(deviceId)) {
			log.debug("Found a connection to device with id: {} for message {}",
					deviceId.hashCode(), payload);
			DeviceConnection connection = connections.get(deviceId);
			connection.send(payload, callback);
		} else {
			log.debug("Did not find device with id hashCode: {} for message {}",
					deviceId.hashCode(), payload);
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
