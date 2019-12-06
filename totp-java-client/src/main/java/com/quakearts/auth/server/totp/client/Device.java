package com.quakearts.auth.server.totp.client;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Device {
	private String id;
	private byte[] seed;
	private long initialCounter;
	private Options options = Options.getInstance();
	private String format = "%0"+options.getOtpLength()+"d";
	
	public Device(String id, byte[] seed, long initialCounter) {
		this.id = id;
		this.seed = seed;
		this.initialCounter = initialCounter;
	}
	
	public String generateOTP() throws GeneralSecurityException {
		return truncatedStringOf(generatedHmacFrom(
				timeValueUsing(System.currentTimeMillis())));
	}
	
	public String generateOTPForTimestamp(long totpTimestamp) throws GeneralSecurityException {
		return truncatedStringOf(generatedHmacFrom(
				timeValueUsing(totpTimestamp)));
	}
	
	private String truncatedStringOf(byte[] hashBytes) {
		int offset = Math.abs(hashBytes[hashBytes.length-1] 
				% (hashBytes.length-4));
		int code = (hashBytes[offset] & 0x7f) << 24 |
				(hashBytes[offset+1] & 0xff) << 16 |
				(hashBytes[offset+2] & 0xff) << 8 |
				hashBytes[offset+3] & 0xff;
		code = (int) (code % Math.pow(10, options.getOtpLength()));
		return String.format(format, code);
	}

	private byte[] generatedHmacFrom(byte[] currentTime) throws GeneralSecurityException {
		Mac mac = Mac.getInstance(options.getMacAlgorithm());
		SecretKey key = new SecretKeySpec(seed, options.getMacAlgorithm());
		mac.init(key);
		mac.update(id.getBytes());
		return mac.doFinal(currentTime);
	}

	private byte[] timeValueUsing(long currentTimeInMillis) {
		long timestamp = (currentTimeInMillis - initialCounter) / options.getTimeStep();
		return ByteBuffer.allocate(8).putLong(timestamp).array();
	}
	
	public String getId() {
		return id;
	}

	public byte[] getSeed() {
		return seed;
	}

	public long getInitialCounter() {
		return initialCounter;
	}
}
