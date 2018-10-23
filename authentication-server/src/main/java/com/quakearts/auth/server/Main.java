package com.quakearts.auth.server;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import javax.enterprise.inject.spi.CDI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.appbase.internal.properties.AppBasePropertiesLoader;
import com.quakearts.appbase.internal.properties.impl.AppBasePropertiesLoaderImpl;
import com.quakearts.appbase.spi.JavaNamingDirectorySpi;
import com.quakearts.appbase.spi.factory.ContextDependencySpiFactory;
import com.quakearts.appbase.spi.factory.EmbeddedWebServerSpiFactory;
import com.quakearts.appbase.spi.factory.JavaNamingDirectorySpiFactory;
import com.quakearts.appbase.spi.impl.JavaNamingDirectorySpiImpl;
import com.quakearts.appbase.spi.impl.TomcatEmbeddedServerSpiImpl;
import com.quakearts.appbase.spi.impl.WeldContextDependencySpiImpl;
import com.quakearts.auth.server.rest.services.DataSourceService;

public class Main {
	
	public static final String AUTHENTICATION_SERVER = "authentication-server";
	public static final String DS_EXTENSTION = "ds.json";
	public static final String DSLOCATION = "etc";
	public static final Logger LOGGER = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	private static Main instance;
	private AppBasePropertiesLoader loader = new AppBasePropertiesLoaderImpl();	
	
	private Main() {}
	
	public static Main getInstance() {
		return instance;
	}
	
    public static void main(String[] args){
    	instance = new Main();    	
    	long start = System.currentTimeMillis();
		instance.startServices(DSLOCATION);
		String message = MessageFormat.format("Started in {0, time, ss.S} seconds",
				System.currentTimeMillis()-start);
		LOGGER.info(message);
    }

    public AppBasePropertiesLoader getLoader() {
		return loader;
	}
        
	private void startServices(String dsLocation) {
		createServices();
		initiateJNDI();
		initiateCDI();
		createDataSources(dsLocation);
		initiateServletContainer();
	}

	private void createServices() {
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

	private void initiateJNDI() {
		JavaNamingDirectorySpi namingSpi = JavaNamingDirectorySpiFactory
			.getInstance()
				.getJavaNamingDirectorySpi();
		namingSpi.initiateJNDIServices();
	}

	private void initiateCDI() {
		ContextDependencySpiFactory
			.getInstance()
			.getContextDependencySpi()
			.initiateContextDependency();
	}

	DataSourceService getDataSourceService(){
		return CDI.current().select(DataSourceService.class).get();
	}
	
	public void createDataSources(String dsLocation) {
		List<File> propertyFiles = loadConfigurationFiles(dsLocation);
		propertyFiles.parallelStream()
			.forEach(propertyFile->getDataSourceService().createUsing(propertyFile));
	}

	private List<File> loadConfigurationFiles(String dsLocation) {
		return loader.listConfigurationFiles(dsLocation, DS_EXTENSTION, AUTHENTICATION_SERVER);
	}

	private void initiateServletContainer() {
		EmbeddedWebServerSpiFactory
			.getInstance()
			.getEmbeddedWebServerSpi()
			.initiateEmbeddedWebServer();
	}
}
