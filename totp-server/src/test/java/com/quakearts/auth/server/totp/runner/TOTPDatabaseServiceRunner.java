package com.quakearts.auth.server.totp.runner;

import javax.enterprise.inject.spi.CDI;

import org.junit.runners.model.InitializationError;

import com.quakearts.auth.server.totp.setup.CreatorService;
import com.quakearts.webtools.test.AllServicesRunner;

public class TOTPDatabaseServiceRunner extends AllServicesRunner {

	public TOTPDatabaseServiceRunner(Class<?> klass) throws InitializationError {
		super(klass);
		CreatorService creatorService = CDI.current()
				.select(CreatorService.class).get();
		
		creatorService.dropAndCreateDatabase();
		creatorService.createEntitiesForTest();
	}

}
