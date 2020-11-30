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

public class TOTPEdgeMain {

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
		initiateServerConnection();
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

	private static void initiateServerConnection() {
		if(Boolean.parseBoolean(System.getProperty("totp.edge.server.connection.active","true"))){
			TOTPServerConnection connection = CDI.current()
					.select(TOTPServerConnection.class).get();
			try {
				LOGGER.debug("Server Connection starting...");
				connection.init();
				LOGGER.debug("Server Connection started");
			} catch (UnrecoverableKeyException | KeyManagementException 
					| KeyStoreException | NoSuchAlgorithmException
					| CertificateException | IOException e) {
				throw new ConfigurationException(e);
			}
		}
	}

	public static void stop(String[] args) {
		EmbeddedWebServerSpiFactory.getInstance()
			.getEmbeddedWebServerSpi().shutdownEmbeddedWebServer();
		System.exit(0);
	}
}
