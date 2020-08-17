package com.quakearts.auth.server.totp.services;

@FunctionalInterface
public interface FallbackListener {
	void fallbackRequested();
}
