package com.quakearts.auth.server.totp.edge;

public interface Callback<T, E extends Exception> {
	void execute(T value) throws E;
}
