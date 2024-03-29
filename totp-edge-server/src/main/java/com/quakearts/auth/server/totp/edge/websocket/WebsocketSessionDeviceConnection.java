package com.quakearts.auth.server.totp.edge.websocket;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnection;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

public class WebsocketSessionDeviceConnection implements DeviceConnection {

	private static final Logger log = LoggerFactory.getLogger(DeviceConnection.class);
	
	public static final String DEVICE_CONNECTION = "WebsocketSessionDeviceConnection.DEVICE_CONNECTION";
	public static final String DEVICE_ID = "WebsocketSessionDeviceConnection.DEVICE_ID";
	public static final String MESSAGE_ID_BASE = "WebsocketSessionDeviceConnection.MESSAGE_ID";
	private Session session;
	private long retrievalTimeout;
	private AtomicLong counter = new AtomicLong();
	
	public WebsocketSessionDeviceConnection(Session session, String deviceId, long retrievalTimeout) {
		this.session = session;
		this.retrievalTimeout = retrievalTimeout;
		session.getUserProperties().put(DEVICE_ID, deviceId);
		session.getUserProperties().put(DEVICE_CONNECTION, this);
	}

	@Override
	public String getDeviceId() {
		return (String) session.getUserProperties().get(DEVICE_ID);
	}

	@Override
	public void send(Payload payload, Consumer<Payload> callback) {
		payload.setId(counter.incrementAndGet());
		log.debug("Sending payload: {} with counter: {}", payload, payload.getId());
		session.getAsyncRemote().sendObject(payload);
		session.getUserProperties().put(MESSAGE_ID_BASE+payload.getId(), callback);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void respond(Payload payload) {
		log.debug("Received response payload with counter: {}", payload.getId());
		Consumer<Payload> callback = (Consumer<Payload>) session
				.getUserProperties().remove(MESSAGE_ID_BASE+payload.getId());
		if(payload.getTimestamp()-System.currentTimeMillis()<=
			retrievalTimeout && callback != null) {
			callback.accept(payload);
			log.debug("Processed response payload with counter: {}", 
					payload.getId());
		} else {
			log.error("Payload with counter: {} timed out", payload.getId());
		}
	}
	
	@Override
	public boolean isConnected() {
		return session.isOpen();
	}
}
