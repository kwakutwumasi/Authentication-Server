package com.quakearts.auth.server.totp.generator;

import com.quakearts.auth.server.totp.model.Device;

public interface KeyGenerator {
	void generateAndStoreIn(Device device);
}
