package com.quakearts.auth.server.totp.channel.impl;

public interface IncomingBitesProcessingListener {
	void processIncoming(byte[] bites);
}
