package com.quakearts.auth.server.totp.edge.websocket.model;

import java.util.Map;
import java.util.Objects;

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
	
	private long id;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, message, timestamp);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Payload other = (Payload) obj;
		return id == other.id && Objects.equals(message, other.message) && timestamp == other.timestamp;
	}
	
}
