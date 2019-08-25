package com.quakearts.auth.server.totp.edge.channel;

import java.nio.ByteBuffer;

public class Message {
	private long ticket;
	private byte[] value;
	
	public Message(long ticket, byte[] value) {
		this.ticket = ticket;
		this.value = value;
	}
	
	public long getTicket() {
		return ticket;
	}
	
	public byte[] getValue() {
		return value;
	}
	
	public byte[] toMessageBytes() {
		byte[] messageBytes = new byte[value.length+8];
		System.arraycopy(ByteBuffer.allocate(8).putLong(ticket).array(), 
				0, messageBytes, 0, 8);
		System.arraycopy(value, 
				0, messageBytes, 8, value.length);
		
		return messageBytes;
	}
}
