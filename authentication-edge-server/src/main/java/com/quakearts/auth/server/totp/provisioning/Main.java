package com.quakearts.auth.server.totp.provisioning;

import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.appbase.spi.JavaNamingDirectorySpi;
import com.quakearts.appbase.spi.factory.ContextDependencySpiFactory;
import com.quakearts.appbase.spi.factory.EmbeddedWebServerSpiFactory;
import com.quakearts.appbase.spi.factory.JavaNamingDirectorySpiFactory;
import com.quakearts.appbase.spi.impl.JavaNamingDirectorySpiImpl;
import com.quakearts.appbase.spi.impl.TomcatEmbeddedServerSpiImpl;
import com.quakearts.appbase.spi.impl.WeldContextDependencySpiImpl;

public class Main {

	public static final Logger LOGGER = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		
    public static void main(String[] args){
    	long start = System.currentTimeMillis();
    	startServices();
		String message = MessageFormat.format("Started in {0, time, ss.S} seconds",
				System.currentTimeMillis()-start);
		LOGGER.info(message);
    }

	private static void startServices() {
		createServices();
		initiateJNDI();
		initiateCDI();
		initiateServletContainer();
	}

	private static void createServices() {
		JavaNamingDirectorySpiFactory
			.getInstance()
			.createJavaNamingDirectorySpi(JavaNamingDirectorySpiImpl.class.getName());
		ContextDependencySpiFactory
			.getInstance()
			.createContextDependencySpi(WeldContextDependencySpiImpl.class.getName());
		EmbeddedWebServerSpiFactory
			.getInstance()
			.createEmbeddedWebServerSpi(TomcatEmbeddedServerSpiImpl.class.getName());
	}

	private static void initiateJNDI() {
		JavaNamingDirectorySpi namingSpi = JavaNamingDirectorySpiFactory
			.getInstance()
				.getJavaNamingDirectorySpi();
		namingSpi.initiateJNDIServices();
	}

	private static void initiateCDI() {
		ContextDependencySpiFactory
			.getInstance()
			.getContextDependencySpi()
			.initiateContextDependency();
	}

	private static void initiateServletContainer() {
		EmbeddedWebServerSpiFactory
			.getInstance()
			.getEmbeddedWebServerSpi()
			.initiateEmbeddedWebServer();
	}

}
