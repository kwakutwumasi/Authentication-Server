package com.quakearts.auth.tests.authentication.test.cdi;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Stack;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.interceptor.Interceptor;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.auth.server.rest.services.DataSourceService;
import com.quakearts.auth.server.rest.services.FileService;
import com.quakearts.auth.server.rest.services.InitialContextService;
import com.quakearts.auth.server.rest.services.impl.DataSourceServiceImpl;
import com.quakearts.auth.server.rest.services.impl.FileServiceImpl;
import com.quakearts.auth.server.rest.services.impl.InitialContextServiceImpl;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
public class AlternativesProducer {
	private FileService fileService;
	private InitialContextService initialContextService;
	private DataSourceService dataSourceService;
	
	private static Stack<File> deletedFiles = new Stack<>();
	private static Stack<File> lastRemoved = new Stack<>();
	
	public static File getLastDeletedFiles() {
		return deletedFiles.pop();
	}
	
	public static File getLastRemoved() {
		return lastRemoved.pop();
	}
	
	@Produces
	public FileService getFileService() {
		if(fileService==null)
			fileService = new FileService() {
				FileServiceImpl fileService = CDI.current().select(FileServiceImpl.class).get();
				
				@Override
				public void saveObjectToFile(Object value, File file) throws IOException {
					if(file.getName().startsWith("testDS")) {
						if(file.getName().contains("SaveError")) {
							throw new IOException("Cannot save file");
						} else {
							return;
						}
					}
					
					fileService.saveObjectToFile(value, file);
				}
				
				@Override
				public boolean fileExists(File file) {
					if(file.getName().startsWith("testDS")) {
						if(file.getName().contains("FileExistsError")) {
							return true;
						} if(file.getName().contains("FileExistsAnd")) {
							return true;
						} else {
							return false;
						} 
					}
					
					return fileService.fileExists(file);
				}
				
				@Override
				public void deleteFile(File file) throws IOException {
					if(file.getName().startsWith("testDS")) {
						if(file.getName().contains("DeleteError")) {
							throw new IOException("Cannot delete file");
						} else {
							deletedFiles.push(file);
							return;
						}		
					}
					fileService.deleteFile(file);
				}
				
				@Override
				public File createFile(String root, String filename) {
					return fileService.createFile(root, filename);
				}
			};
		
		return fileService;
	}
	
	@Produces
	public InitialContextService getInitialContextService() {
		if(initialContextService == null)
			initialContextService = new InitialContextService() {	
				InitialContextServiceImpl serviceImpl = CDI.current()
						.select(InitialContextServiceImpl.class).get();
				
				@Override
				public void unbind(String jndiName) throws NamingException {
					if(jndiName.startsWith("java:/jdbc/TestDS")) {
						if(jndiName.contains("UnbindError")) {
							throw new NamingException("Unable to unbind Name");
						}
					}
					serviceImpl.unbind(jndiName);
				}
				
				@SuppressWarnings("unchecked")
				@Override
				public <T> T lookup(final String jndiName) throws NamingException {
					if(jndiName.startsWith("java:/jdbc/TestDS")) {
						if(jndiName.contains("LookupError")) {
							throw new NamingException("Unable to lookup Name");
						} else {
							return (T) MockingProxyBuilder.createMockingInvocationHandlerFor(DataSource.class)
									.mock("getConnection").withEmptyMethod(()->{
										if(jndiName.contains("GetConnectionError")) {
											throw new SQLException("Cannot connect");
										} else {
											return MockingProxyBuilder.createMockingInvocationHandlerFor(Connection.class)
													.mock("close").withVoidEmptyMethod(()->{})
													.mock("getCatalog").withEmptyMethod(()->{
														if(jndiName.contains("GetCatalogError"))
															throw new SQLException("Cannot connect to catalog");

														return null;
													}).thenBuild();
										}
									}).thenBuild();
						}
					}
					return serviceImpl.lookup(jndiName);
				}
				
				@Override
				public void createContext(String contextName) throws NamingException {
					serviceImpl.createContext(contextName);
				}
				
				@Override
				public void bind(String jndiName, Object object) throws NamingException {
					if(jndiName.startsWith("java:/jdbc/TestDS")) {
						if(jndiName.contains("BindError")) {
							throw new NamingException("Unable to bind name");
						}
						return;
					}
					serviceImpl.bind(jndiName, object);
				}
			};
			
		return initialContextService;
	}
	
	@Produces
	public DataSourceService getDataSourceService() {
		if(dataSourceService==null)
			dataSourceService = new DataSourceService() {
				DataSourceServiceImpl serviceImpl = CDI.current()
						.select(DataSourceServiceImpl.class).get();
				
				@Override
				public void removeIdentifiedBy(File dataSourceFile) {
					if(dataSourceFile.getName().startsWith("testDS")) {
						if(dataSourceFile.getName().contains("RemoveError")) {
							throw new ConfigurationException("Error removing datasource");
						} else {
							lastRemoved.push(dataSourceFile);
							return;
						}
					}
					serviceImpl.removeIdentifiedBy(dataSourceFile);
				}
				
				@Override
				public List<String> getAllDataSources() {
					return serviceImpl.getAllDataSources();
				}
				
				@Override
				public void createUsing(File propertyFile) {
					if(propertyFile.getName().startsWith("testDS")) {
						if(propertyFile.getName().contains("ConfigError")) {
							throw new ConfigurationException("Error configuring datasource");
						} else {
							return;
						}
					}
					
					serviceImpl.createUsing(propertyFile);
				}
			};
			
		return dataSourceService;
	}
}
