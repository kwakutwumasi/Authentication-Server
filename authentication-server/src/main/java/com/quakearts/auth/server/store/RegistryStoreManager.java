package com.quakearts.auth.server.store;

import org.infinispan.Cache;

import com.quakearts.auth.server.rest.models.Registration;

public interface RegistryStoreManager {
	Cache<String, Registration> getCache();
	Cache<String, String> getAliasCache();
	Cache<String, String> getSecretsCache();
}
