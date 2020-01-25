package com.quakearts.auth.server.totp.device;

import java.util.concurrent.ExecutorService;

public interface DeviceConnectionExecutorService {
	ExecutorService getExecutorService();
}
