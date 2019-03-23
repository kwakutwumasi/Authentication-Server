package com.quakearts.auth.server.totp.authentication;

import com.quakearts.auth.server.totp.model.Device;

public interface AuthenticationService {
	boolean authenticate(Device device, String totpToken);
	boolean isLocked(Device device);
}
