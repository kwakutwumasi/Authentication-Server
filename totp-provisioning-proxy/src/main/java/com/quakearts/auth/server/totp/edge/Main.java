package com.quakearts.auth.server.totp.edge;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.MessageFormat;

import javax.enterprise.inject.spi.CDI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.appbase.spi.JavaNamingDirectorySpi;
import com.quakearts.appbase.spi.factory.ContextDependencySpiFactory;
import com.quakearts.appbase.spi.factory.EmbeddedWebServerSpiFactory;
import com.quakearts.appbase.spi.factory.JavaNamingDirectorySpiFactory;
import com.quakearts.appbase.spi.impl.JavaNamingDirectorySpiImpl;
import com.quakearts.appbase.spi.impl.TomcatEmbeddedServerSpiImpl;
import com.quakearts.appbase.spi.impl.WeldContextDependencySpiImpl;
import com.quakearts.auth.server.totp.edge.channel.TOTPServerConnection;

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
		TOTPServerConnection connection = CDI.current()
				.select(TOTPServerConnection.class).get();
		try {
			connection.init();
		} catch (UnrecoverableKeyException | KeyManagementException 
				| KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException e) {
			throw new ConfigurationException(e);
		}
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

	public static void stop(String[] args) {
		System.exit(0);
	}
}
