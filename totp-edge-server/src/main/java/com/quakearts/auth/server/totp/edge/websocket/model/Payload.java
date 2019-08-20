package com.quakearts.auth.server.totp.edge.websocket.model;

import java.util.Map;

public class Payload {
	private Map<String, String> message;

	public Map<String, String> getMessage() {
		return message;
	}

	public void setMessage(Map<String, String> message) {
		this.message = message;
	}
	
	private long timestamp = System.currentTimeMillis();
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
