package com.quakearts.auth.server.totp.test;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.appbase.spi.factory.ContextDependencySpiFactory;
import com.quakearts.appbase.spi.factory.DataSourceProviderSpiFactory;
import com.quakearts.appbase.spi.factory.EmbeddedWebServerSpiFactory;
import com.quakearts.appbase.spi.factory.JavaNamingDirectorySpiFactory;
import com.quakearts.appbase.spi.factory.JavaTransactionManagerSpiFactory;
import com.quakearts.appbase.spi.impl.AtomikosBeanDatasourceProviderSpiImpl;
import com.quakearts.appbase.spi.impl.AtomikosJavaTransactionManagerSpiImpl;
import com.quakearts.appbase.spi.impl.JavaNamingDirectorySpiImpl;
import com.quakearts.appbase.spi.impl.TomcatEmbeddedServerSpiImpl;
import com.quakearts.appbase.spi.impl.WeldContextDependencySpiImpl;

public class TestMain {

	public static final Logger LOGGER = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    public static void main(String[] args){
    	long start = System.currentTimeMillis();
		JavaNamingDirectorySpiFactory.getInstance().createJavaNamingDirectorySpi(JavaNamingDirectorySpiImpl.class.getName());
		JavaTransactionManagerSpiFactory.getInstance().createJavaTransactionManagerSpi(AtomikosJavaTransactionManagerSpiImpl.class.getName());
		DataSourceProviderSpiFactory.getInstance().createDataSourceProviderSpi(AtomikosBeanDatasourceProviderSpiImpl.class.getName());
		ContextDependencySpiFactory.getInstance().createContextDependencySpi(WeldContextDependencySpiImpl.class.getName());
		EmbeddedWebServerSpiFactory.getInstance().createEmbeddedWebServerSpi(TomcatEmbeddedServerSpiImpl.class.getName());
		
		JavaNamingDirectorySpiFactory.getInstance().getJavaNamingDirectorySpi().initiateJNDIServices();
		JavaTransactionManagerSpiFactory.getInstance().getJavaTransactionManagerSpi().initiateJavaTransactionManager();
		DataSourceProviderSpiFactory.getInstance().getDataSourceProviderSpi().initiateDataSourceSpi();
		ContextDependencySpiFactory.getInstance().getContextDependencySpi().initiateContextDependency();
		EmbeddedWebServerSpiFactory.getInstance().getEmbeddedWebServerSpi().initiateEmbeddedWebServer();
		
		LOGGER.info(MessageFormat.format("Started in {0, time, ss.S} seconds", System.currentTimeMillis()-start));
	}

}
