package com.quakearts.auth.server.totp.generator;

import com.quakearts.auth.server.totp.model.Device;

public interface TOTPGenerator {
	String[] generateFor(Device device, long currentTimeInMillis);
}
