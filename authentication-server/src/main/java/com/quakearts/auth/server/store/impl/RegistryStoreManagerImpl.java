package com.quakearts.auth.server.store.impl;

import java.util.Arrays;

import javax.enterprise.inject.Produces;

import org.infinispan.Cache;
import org.infinispan.commons.configuration.ClassWhiteList;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionType;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import com.quakearts.auth.server.rest.models.Registration;
import com.quakearts.auth.server.store.RegistryStoreManager;
import com.quakearts.auth.server.store.annotation.AliasStore;
import com.quakearts.auth.server.store.annotation.RegistryStore;
import com.quakearts.auth.server.store.annotation.SecretsStore;

public class RegistryStoreManagerImpl implements RegistryStoreManager {
	private static EmbeddedCacheManager cacheContainer;

	private static CacheContainer getCacheContainer() {
		if (cacheContainer == null) {
			cacheContainer = new DefaultCacheManager(new GlobalConfigurationBuilder()
					.serialization()
					.marshaller(new JavaSerializationMarshaller(new ClassWhiteList(Arrays
							.asList("com.quakearts.auth.server.rest.models.*","java.util.ArrayList"))))
					.defaultCacheName("global.default")
					.build(),
					new ConfigurationBuilder()
					.memory()
						.evictionType(EvictionType.COUNT)
						.size(10)
					.persistence()
						.passivation(false)
						.addSingleFileStore()
							.location("authentication-server-store")
								.async()
								.enable()
								.preload(false)
								.shared(false)
					.build());

			cacheContainer.start();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				if (cacheContainer != null) {
					cacheContainer.stop();
				}
			}));

		}
		return cacheContainer;
	}

	@Override
	@Produces @RegistryStore
	public Cache<String, Registration> getCache() {
		return getCacheContainer().getCache("default");
	}

	@Override
	@Produces @AliasStore
	public Cache<String, String> getAliasCache() {
		return getCacheContainer().getCache("alias-store");
	}
	
	@Override
	@Produces @SecretsStore
	public Cache<String, String> getSecretsCache() {
		return getCacheContainer().getCache("secrets-store");
	}
}