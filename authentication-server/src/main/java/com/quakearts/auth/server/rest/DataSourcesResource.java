package com.quakearts.auth.server.rest;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.auth.server.Main;
import com.quakearts.auth.server.rest.services.DataSourceService;
import com.quakearts.auth.server.rest.services.ErrorService;
import com.quakearts.auth.server.rest.services.FileService;
import com.quakearts.auth.server.rest.services.InitialContextService;
import com.quakearts.auth.server.rest.services.OptionsService;
import com.quakearts.auth.server.rest.validators.annotation.ValidDataSourceKey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Singleton
@Path("datasource")
@Produces(MediaType.APPLICATION_JSON)
public class DataSourcesResource {
		
	@Inject
	private FileService fileService;
	@Inject
	private InitialContextService initialContextService;
	@Inject
	private DataSourceService dataSourceService;
	@Inject
	private OptionsService optionsService;
	@Inject
	private ErrorService errorService;
	
	@Operation(summary="Return a list of all currently configured datasources")
	@ApiResponse(responseCode="200",
		description="Retrieval succeeded",
		content=@Content(array=@ArraySchema(schema=@Schema(implementation=String.class))))
	@GET
	public List<String> listAllDataSources() {
		return dataSourceService.getAllDataSources();
	}
	
	@Operation(summary="Create a data source using the options provided",
			description="An SQL Ddata source connection is required for database authentication. "
					+ "This resource makes it possible to dynamically create SQL data sources for use with "
					+ "authentication modules. This services makes use of the secrets service to replace "
					+ "sensitive information, making it possible to upload a configuration without sensitive "
					+ "information.")
	@ApiResponse(responseCode="204",
		description="Data source addition succeeded")
	@ApiResponse(responseCode="417",
			description="There was a problem processing the data source",
			content=@Content(schema=@Schema(
					implementation=com.quakearts.auth.server.rest.models.ErrorResponse.class,
			description="A JSON object describing and explaining the error",
			example="{\n" + 
					"    \"code\": \"existing-id\",\n" + 
					"    \"explanations\": [\n" + 
					"        \"A registration with the provided ID/alias already exists\"\n" + 
					"    ]\n" + 
					"}")))	
	@PUT
	@Path("{datasourcekey}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void addDatasource(@ValidDataSourceKey @PathParam("datasourcekey") final String datasourcekey,
			final Map<String, Object> configuration, @Suspended final AsyncResponse asyncResponse) {
		CompletableFuture.runAsync(()->{
			optionsService.resolveSecrets(configuration);
			File dataSourceFile = createDataSourceFile(datasourcekey);
			if(saveDataSourceFile(dataSourceFile, configuration, asyncResponse) 
					&& createDataSource(asyncResponse, dataSourceFile)
					&& testDataSource(configuration, asyncResponse, dataSourceFile))
				asyncResponse.resume(Response.noContent().build());
		});
	}

	private File createDataSourceFile(final String datasourcekey) {
		return fileService.createFile(Main.DSLOCATION, datasourcekey+"."+Main.DS_EXTENSTION);
	}

	private boolean saveDataSourceFile(final File dataSourceFile, final Map<String, Object> configuration,
			final AsyncResponse asyncResponse) {
		if(fileService.fileExists(dataSourceFile)) {
			return respondWithError(asyncResponse, new Exception("The datasource key already exists"));
		}
		
		try {
			fileService.saveObjectToFile(configuration, dataSourceFile);
			return true;
		} catch (IOException e) {
			return respondWithError(asyncResponse, e);
		}
	}

	private boolean createDataSource(final AsyncResponse asyncResponse, File dataSourceFile) {
		try {
			dataSourceService.createUsing(dataSourceFile);
			return true;
		} catch (ConfigurationException e) {
			return deleteFileAndReturnErrorResponse(asyncResponse, dataSourceFile, e);
		}
	}

	private boolean testDataSource(final Map<String, Object> configuration, final AsyncResponse asyncResponse,
			File dataSourceFile) {
		
		String dataSourceName = "java:/jdbc/"+configuration.get("jndi.name");
		DataSource dataSource = null;
		try {
			 dataSource = initialContextService.lookup(dataSourceName);
		} catch (NamingException e) {
			return deleteFileAndReturnErrorResponse(asyncResponse, dataSourceFile, e);
		}
		
		return testConnection(asyncResponse, dataSourceFile, dataSource);
	}

	private boolean testConnection(final AsyncResponse asyncResponse, File dataSourceFile,
			DataSource dataSource) {
		try(Connection connection = dataSource.getConnection()) {
			connection.getCatalog();
			return true;
		} catch (SQLException e) {
			try {
				dataSourceService.removeIdentifiedBy(dataSourceFile);
				return deleteFileAndReturnErrorResponse(asyncResponse, dataSourceFile, e);
			} catch (ConfigurationException e2) {
				return deleteFileAndReturnErrorResponse(asyncResponse, dataSourceFile, e, e2);
			}
		}
	}

	private boolean deleteFileAndReturnErrorResponse(final AsyncResponse asyncResponse, 
			final File dataSourceFile, Exception... exceptions) {
		try {
			fileService.deleteFile(dataSourceFile);
		} catch (IOException e) {
			exceptions = Arrays.copyOf(exceptions, exceptions.length+1);
			exceptions[exceptions.length-1] = e;
		} 
		
		return respondWithError(asyncResponse, exceptions);
	}

	private boolean respondWithError(final AsyncResponse asyncResponse, final Exception... exceptions) {
		asyncResponse.resume(new WebApplicationException(Response
				.status(Status.EXPECTATION_FAILED)
				.type(MediaType.APPLICATION_JSON)
				.entity(errorService
						.createErrorResponse("datasource-error", exceptions)).build()));
		return false;
	}

	@Operation(summary="Remove a data source identified by the key")
	@ApiResponse(responseCode="204",
		description="Data source removal succeeded")
	@ApiResponse(responseCode="404",
		description="The data source identified by the key cannot be found",
		content=@Content(schema=@Schema(
				implementation=com.quakearts.auth.server.rest.models.ErrorResponse.class,
		description="A JSON object describing and explaining the error",
		example="{\n" + 
				"    \"code\": \"existing-id\",\n" + 
				"    \"explanations\": [\n" + 
				"        \"A registration with the provided ID/alias already exists\"\n" + 
				"    ]\n" + 
				"}")))	
	@ApiResponse(responseCode="417",
			description="There was a problem processing the data source removal",
			content=@Content(schema=@Schema(
					implementation=com.quakearts.auth.server.rest.models.ErrorResponse.class,
			description="A JSON object describing and explaining the error",
			example="{\n" + 
					"    \"code\": \"existing-id\",\n" + 
					"    \"explanations\": [\n" + 
					"        \"A registration with the provided ID/alias already exists\"\n" + 
					"    ]\n" + 
					"}")))	
	@DELETE
	@Path("{datasourcekey}")
	public void removeDatasource(@PathParam("datasourcekey") final String datasourcekey, 
			@Suspended final AsyncResponse asyncResponse) {
		CompletableFuture.runAsync(()->{
			File dataSourceFile = createDataSourceFile(datasourcekey);
			if(!fileService.fileExists(dataSourceFile)) {
				respondNotFound(asyncResponse);
			} else {
				if(doRemoveDataSource(asyncResponse, dataSourceFile))
					asyncResponse.resume(Response.noContent().build());
			}
		});
	}

	private boolean doRemoveDataSource(final AsyncResponse asyncResponse, File dataSourceFile) {
		return removeDataSourceFromService(asyncResponse, dataSourceFile) 
				&& deleteFile(asyncResponse, dataSourceFile);
	}

	private boolean removeDataSourceFromService(final AsyncResponse asyncResponse, File dataSourceFile) {
		try {
			dataSourceService.removeIdentifiedBy(dataSourceFile);
			return true;
		} catch (ConfigurationException e) {
			 return respondWithError(asyncResponse, e);
		}
	}

	private boolean deleteFile(final AsyncResponse asyncResponse, File dataSourceFile) {
		 try {
			fileService.deleteFile(dataSourceFile);
		} catch (IOException e) {
			 return respondWithError(asyncResponse, e);
		}
		 return true;
	}

	private boolean respondNotFound(final AsyncResponse asyncResponse) {
		asyncResponse.resume(new WebApplicationException(Response.status(Status.NOT_FOUND)
				.type(MediaType.APPLICATION_JSON)
				.entity(errorService.createErrorResponse("datasource-error",
						"The data source file could not be found")).build()));
		return false;
	}
}
