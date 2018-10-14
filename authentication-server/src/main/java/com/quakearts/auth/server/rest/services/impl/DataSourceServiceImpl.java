package com.quakearts.auth.server.rest.services.impl;

import java.beans.IntrospectionException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.NamingException;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.appbase.internal.properties.AppBasePropertiesLoader;
import com.quakearts.appbase.internal.properties.ConfigurationPropertyMap;
import com.quakearts.auth.server.Main;
import com.quakearts.auth.server.rest.services.DataSourceService;
import com.quakearts.auth.server.rest.services.InitialContextService;

@Singleton
public class DataSourceServiceImpl implements DataSourceService {

	@Inject
	private InitialContextService contextService;
	
	private List<String> allDataSources = Collections.synchronizedList(new ArrayList<>());

    public List<String> getAllDataSources() {
		return allDataSources;
	}
    
    @PostConstruct
    public void createContext() {
    	try {
			contextService.createContext("java:/jdbc");
		} catch (NamingException e) {
			throw new ConfigurationException(e);
		}
    }
	
	@Override
	public void createUsing(File propertyFile) {
		ConfigurationPropertyMap propertyMap = loadConfiguration(Main.getInstance().getLoader(), propertyFile);
		createDataSource(propertyMap, propertyFile.getName());
	}

	private ConfigurationPropertyMap loadConfiguration(AppBasePropertiesLoader loader, File propertyFile) {
		ConfigurationPropertyMap propertyMap = loader.loadParametersFromFile(propertyFile);
		return propertyMap;
	}
	
	private void createDataSource(ConfigurationPropertyMap propertyMap, String propertyfile) {
		String dataSourceName = getDataSourceName(propertyMap, propertyfile);
		PoolProperties poolProperties = createPoolProperties(propertyMap);
		createTomcatDataSource(dataSourceName, poolProperties);
	}

	private String getDataSourceName(ConfigurationPropertyMap propertyMap, String propertyfile) {
		String dataSourceName = propertyMap.getString("jndi.name");
		if(dataSourceName == null || dataSourceName.trim().isEmpty())
			throw new ConfigurationException("Missing parameter 'jndi.name' in "+propertyfile);
		return dataSourceName;
	}

	private PoolProperties createPoolProperties(ConfigurationPropertyMap propertyMap) {
		PoolProperties poolProperties = new PoolProperties();
		try {
			propertyMap.populateBean(poolProperties, null);
		} catch (IntrospectionException e) {
			throw new ConfigurationException(e);
		}
		return poolProperties;
	}

	private void createTomcatDataSource(String dataSourceName, PoolProperties poolProperties) {
		DataSource dataSource = new DataSource(poolProperties);
		try {
			String jndiName = createJndiName(dataSourceName);
			bindDataSource(dataSource, jndiName);
			allDataSources.add(jndiName);
		} catch (NamingException e) {
			throw new ConfigurationException("Unable to bind datasource "+dataSourceName, e);
		}
	}

	public String createJndiName(String dataSourceName) {
		return "java:/jdbc/"+dataSourceName;
	}

	private void bindDataSource(DataSource dataSource, String jndiName) throws NamingException {
		contextService.bind(jndiName, dataSource);
	}
	
	@Override
	public void removeIdentifiedBy(File dataSourceFile) {
		String dataSourceName = getDataSourceName(dataSourceFile);
		String jndiName = createJndiName(dataSourceName);
		try {
			unbindDataSource(jndiName);
			allDataSources.remove(jndiName);
		} catch (NamingException e) {
			throw new ConfigurationException("Unable to unbind datasource "+jndiName, e);
		}
	}

	private String getDataSourceName(File dataSourceFile) {
		ConfigurationPropertyMap configuration = Main.getInstance()
				.getLoader().loadParametersFromFile(dataSourceFile);
		String dataSourceName = configuration.getString("jndi.name");
		return dataSourceName;
	}

	private void unbindDataSource(String jndiName) throws NamingException {
		contextService.unbind(jndiName);
	}

}
