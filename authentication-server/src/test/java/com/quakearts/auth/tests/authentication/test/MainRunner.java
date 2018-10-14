package com.quakearts.auth.tests.authentication.test;

import javax.enterprise.inject.spi.CDI;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.quakearts.appbase.spi.factory.ContextDependencySpiFactory;
import com.quakearts.appbase.spi.factory.EmbeddedWebServerSpiFactory;
import com.quakearts.appbase.spi.factory.JavaNamingDirectorySpiFactory;
import com.quakearts.auth.server.Main;

public class MainRunner extends BlockJUnit4ClassRunner {

	public MainRunner(Class<?> klass) throws InitializationError {
		super(klass);
		if(Main.getInstance()==null)
			Main.main(new String[]{});
		
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			EmbeddedWebServerSpiFactory
				.getInstance().getEmbeddedWebServerSpi()
				.shutdownEmbeddedWebServer();
			ContextDependencySpiFactory
				.getInstance().getContextDependencySpi()
				.shutDownContextDependency();
			JavaNamingDirectorySpiFactory
				.getInstance().getJavaNamingDirectorySpi()
				.shutdownJNDIService();
		}));
	}

	@Override
	protected Object createTest() throws Exception {
		return CDI.current().select(getTestClass().getJavaClass()).get();
	}
}
