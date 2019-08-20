package com.quakearts.auth.server.totp.edge.websocket;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.websocket.Session;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnection;
import com.quakearts.auth.server.totp.edge.exception.CapacityExceededException;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

public class WebsocketSessionDeviceConnection implements DeviceConnection {

	public static final String DEVICE_CONNECTION = "WebsocketSessionDeviceConnection.DEVICE_CONNECTION";
	public static final String DEVICE_ID = "WebsocketSessionDeviceConnection.DEVICE_ID";
	private Session session;
	private long retrievalTimeout;
	private BlockingQueue<Payload> payloads;
	
	public WebsocketSessionDeviceConnection(Session session, String deviceId, long retrievalTimeout, int queueBounds) {
		this.session = session;
		this.retrievalTimeout = retrievalTimeout;
		this.payloads = new LinkedBlockingQueue<>(queueBounds);
		session.getUserProperties().put(DEVICE_ID, deviceId);
		session.getUserProperties().put(DEVICE_CONNECTION, this);
	}

	@Override
	public String getDeviceId() {
		return (String) session.getUserProperties().get(DEVICE_ID);
	}

	@Override
	public void send(Payload payload) {
		session.getAsyncRemote().sendObject(payload);
	}

	@Override
	public void respond(Payload payload) throws CapacityExceededException {
		if(payload.getTimestamp()-System.currentTimeMillis()>
			retrievalTimeout) {
			return;
		}
		
		if(!payloads.offer(payload)) {
			throw new CapacityExceededException();
		}
	}

	@Override
	public Payload retrieve() {
		try {
			return payloads.poll(retrievalTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}
}
