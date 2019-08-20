package com.quakearts.auth.server.totp.edge.channel;

import com.quakearts.auth.server.totp.edge.exception.CapacityExceededException;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

public interface DeviceConnection {
	String getDeviceId();
	void send(Payload payload);
	void respond(Payload payload) throws CapacityExceededException;
	Payload retrieve();
}
