package com.quakearts.auth.server.totp.edge.channel;

import java.util.function.Consumer;

import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

public interface DeviceConnection {
	String getDeviceId();
	void send(Payload payload, Consumer<Payload> callback);
	void respond(Payload payload);
}
