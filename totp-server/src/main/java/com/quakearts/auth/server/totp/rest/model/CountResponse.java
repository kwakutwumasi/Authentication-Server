package com.quakearts.auth.server.totp.rest.model;

public class CountResponse {
	private long count;
	
	public long getCount() {
		return count;
	}
	
	public void setCount(long count) {
		this.count = count;
	}
	
	public CountResponse withCountAs(long count) {
		setCount(count);
		return this;
	}
}
