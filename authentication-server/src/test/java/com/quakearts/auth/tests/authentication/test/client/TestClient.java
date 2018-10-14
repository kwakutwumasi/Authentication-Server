package com.quakearts.auth.tests.authentication.test.client;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.rest.models.Secret;
import com.quakearts.rest.client.HttpClient;
import com.quakearts.rest.client.HttpResponse;
import com.quakearts.rest.client.HttpVerb;
import com.quakearts.rest.client.exception.HttpClientException;

public class TestClient extends HttpClient {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5109123356929777991L;
	private ObjectMapper objectMapper = new ObjectMapper();
	private HttpResponse httpResponse;
	
	public HttpResponse getHttpResponse() {
		return httpResponse;
	}
	
	public class TestClientException extends HttpClientException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 3932019191874360287L;
		private ErrorResponse errorResponse;

		public TestClientException(String message, ErrorResponse errorResponse) {
			super(message);
			this.errorResponse=errorResponse;
		}
		
		public ErrorResponse getErrorResponse() {
			return errorResponse;
		}
	}
	
	public Registration getById(String id) throws IOException, HttpClientException {
		httpResponse = sendRequest("/registration/"+id, 
				null, HttpVerb.GET, null);
		
		if(httpResponse.getHttpCode()==200) {
			return objectMapper.readValue(httpResponse.getOutput(), Registration.class);
		} else {
			throw processError(httpResponse);
		}
	}
	
	public void addSecretValue(Secret secret) throws IOException, HttpClientException {
		httpResponse = sendRequest("/secrets", objectMapper
				.writeValueAsString(secret), HttpVerb.PUT, "application/json");
		
		if(httpResponse.getHttpCode()>299) {
			throw processError(httpResponse);
		}
	}
	
	TypeReference<List<String>> reference = new TypeReference<List<String>>() {};
	
	public List<String> listSecretValues() throws IOException, HttpClientException {
		httpResponse = sendRequest("/secrets", null, HttpVerb.GET, null);
		
		if(httpResponse.getHttpCode() == 200) {
			return objectMapper.readValue(httpResponse.getOutput(), reference);
		} else {
			throw processError(httpResponse);
		}
	}
	
	public void removeSecretValue(String key) throws IOException, HttpClientException {
		httpResponse = sendRequest("/secrets/"+URLEncoder.encode(key, "UTF-8"), 
				null, HttpVerb.DELETE, null);
		
		if(httpResponse.getHttpCode()>299) {
			throw processError(httpResponse);
		}
	}

	private TestClientException processError(HttpResponse httpResponse)
			throws IOException, JsonParseException, JsonMappingException, TestClientException {
		ErrorResponse errorResponse = new ErrorResponse();
		if(httpResponse.getOutput()!=null)
			errorResponse = objectMapper.
					readValue(httpResponse.getOutput(), ErrorResponse.class);
		return new TestClientException("Error", errorResponse);
	}
	
	public void register(Registration registration) 
			throws  IOException, HttpClientException {
		write(registration, null, HttpVerb.POST);
	}

	public void update(String id, Registration registration) 
			throws IOException, HttpClientException {
		write(registration,id,HttpVerb.PUT);
	}
	
	private void write(Registration registration, String id, HttpVerb httpVerb) 
				throws IOException, HttpClientException {
		httpResponse = sendRequest("/registration"
				+(id!=null?"/"+id:""),
				objectMapper.writeValueAsString(registration), httpVerb, 
				"application/json");
		
		if(httpResponse.getHttpCode()>299) {
			throw processError(httpResponse);
		}
	}
	
	public void delete(String id) 
			throws IOException, HttpClientException {
		httpResponse = sendRequest("/registration/"+id, 
				null, HttpVerb.DELETE, null);
		
		if(httpResponse.getHttpCode()>299) {
			throw processError(httpResponse);
		}
	}
	
	public void createDataSource(String dataSourceKey, Map<String, String> configuration) 
			throws IOException, HttpClientException {
		httpResponse = sendRequest("/datasource/"+dataSourceKey, 
				objectMapper.writeValueAsString(configuration), 
				HttpVerb.PUT, "application/json");
		
		if(httpResponse.getHttpCode()>299) {
			throw processError(httpResponse);
		}
	}

	public void removeDataSource(String dataSourceKey) 
			throws IOException, HttpClientException {
		httpResponse = sendRequest("/datasource/"+dataSourceKey, 
				null, HttpVerb.DELETE, null);
		
		if(httpResponse.getHttpCode()>299) {
			throw processError(httpResponse);
		}
	}

	public List<String> listDataSources() 
			throws IOException, HttpClientException {
		httpResponse = sendRequest("/datasource", 
				null, HttpVerb.GET, null);
		
		if(httpResponse.getHttpCode()==200) {
			List<String> datasources = objectMapper.readValue(httpResponse.getOutput(), 
					new TypeReference<List<String>>() {});
			
			return datasources;
		} else {
			throw processError(httpResponse);
		}
	}
	
	public TokenResponse authenticate(AuthenticationRequest request) 
			throws IOException, HttpClientException {
		httpResponse = sendRequest("/authenticate/"+request, null, 
				HttpVerb.GET, null);
		if(httpResponse.getHttpCode()==200) {
			return objectMapper.readValue(httpResponse.getOutput(), 
					TokenResponse.class);
		} else {
			throw processError(httpResponse);
		}
	}
	
	public static class AuthenticationRequest {
		private String alias;
		private String application;
		private String client;
		private String credential;

		public String getAlias() {
			return alias;
		}

		public AuthenticationRequest setAliasAs(String alias) {
			this.alias = alias;
			return this;
		}
		
		public String getApplication() {
			return application;
		}

		public AuthenticationRequest setApplicationAs(String application) {
			this.application = application;
			return this;
		}

		public String getClient() {
			return client;
		}

		public AuthenticationRequest setClientAs(String client) {
			this.client = client;
			return this;
		}

		public String getCredential() {
			return credential;
		}
		
		public AuthenticationRequest setCredentialAs(String credential) {
			this.credential = credential;
			return this;
		}

		@Override
		public String toString() {
			return MessageFormat.format("{0}/{1}?clientId={2}&credential={3}", 
					alias, application, client, credential);
		}
	}
}