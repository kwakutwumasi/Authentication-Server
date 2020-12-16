package com.quakearts.auth.server.totp.edge.websocket.model;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.quakearts.webapp.security.util.HashPassword;

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

	@Override
	public String toString() {
		return "\n[\n\tmessage=" + mask(message) + ",\n\ttimestamp=" + timestamp + ",\n\tid=" + id + "\n]\n";
	}
	
	private String mask(Map<String, String> message){
		return "{"+message.entrySet().stream().map(entry->"'"+entry.getKey()+"' = '" +mask(entry.getValue())+"'")
				.collect(Collectors.joining(", "))+"}";
	}
	
	private String mask(String value){
		return new HashPassword(value, "SHA-1", 0, "")
				.toString().toUpperCase();
	}
}
