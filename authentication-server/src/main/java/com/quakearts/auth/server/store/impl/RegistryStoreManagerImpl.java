package com.quakearts.auth.server.store.impl;

import java.util.Arrays;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.infinispan.Cache;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.configuration.ClassWhiteList;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionType;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import com.quakearts.auth.server.rest.models.Registration;
import com.quakearts.auth.server.store.RegistryStoreManager;
import com.quakearts.auth.server.store.annotation.AliasStore;
import com.quakearts.auth.server.store.annotation.RegistryStore;
import com.quakearts.auth.server.store.annotation.SecretsStore;

@Singleton
public class RegistryStoreManagerImpl implements RegistryStoreManager {
	private EmbeddedCacheManager embeddedCacheManager;
	private Configuration configuration;

	private EmbeddedCacheManager getEmbeddedCacheManager() {
		if (embeddedCacheManager == null) {
			
			configuration = new ConfigurationBuilder()
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
							.segmented(false)
							.shared(false)
				.build();
			
			embeddedCacheManager = new DefaultCacheManager(new GlobalConfigurationBuilder()
					.serialization()
					.marshaller(new JavaSerializationMarshaller(new ClassWhiteList(Arrays
							.asList("com.quakearts.auth.server.rest.models.*","java.util.*"))))
					.defaultCacheName("global.default")
					.build(), configuration);

			embeddedCacheManager.start();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				if (embeddedCacheManager != null) {
					embeddedCacheManager.stop();
				}
			}));

		}
		return embeddedCacheManager;
	}

	@Override
	@Produces @RegistryStore
	public Cache<String, Registration> getCache() {
		return getCacheByName("default");
	}

	@Override
	@Produces @AliasStore
	public Cache<String, String> getAliasCache() {
		return getCacheByName("alias-store");
	}
	
	@Override
	@Produces @SecretsStore
	public Cache<String, String> getSecretsCache() {
		return getCacheByName("secrets-store");
	}
	
	private <T> Cache<String, T> getCacheByName(String name) {
		try {
			return getEmbeddedCacheManager().getCache(name);
		} catch (CacheConfigurationException e) {
			getEmbeddedCacheManager().defineConfiguration(name, configuration);
			return getCacheByName(name);
		}
	}
}