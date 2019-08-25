package com.quakearts.auth.server.totp.function;

@FunctionalInterface
public interface CheckedConsumer<T, E extends Exception> {
	void accept(T value) throws E;
}