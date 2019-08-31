package com.quakearts.auth.server.totp.rest.authorization;

import java.util.concurrent.ExecutorService;

public interface AuthorizationExecutorService {
	ExecutorService getExecutorService();
}
