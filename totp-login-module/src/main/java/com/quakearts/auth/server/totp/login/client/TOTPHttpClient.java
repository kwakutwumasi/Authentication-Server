package com.quakearts.auth.server.totp.login.client;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.totp.login.client.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.login.client.model.DirectAuthenticationRequest;
import com.quakearts.auth.server.totp.login.client.model.ErrorResponse;
import com.quakearts.auth.server.totp.login.exception.ConnectorException;
import com.quakearts.rest.client.HttpObjectClient;
import com.quakearts.rest.client.HttpResponse;
import com.quakearts.rest.client.HttpVerb;
import com.quakearts.rest.client.exception.HttpClientException;

public class TOTPHttpClient extends HttpObjectClient {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2968088214812254664L;
	static final String APPLICATION_JSON = "application/json";
	private ObjectMapper objectMapper = new ObjectMapper();
	protected String file;
	
	public void authenticate(AuthenticationRequest request) 
			throws IOException, HttpClientException {
		execute(HttpVerb.POST, file+"/authenticate", request, APPLICATION_JSON, null);
	}

	public void authenticateDirectly(DirectAuthenticationRequest request) 
			throws IOException, HttpClientException {
		execute(HttpVerb.POST, file+"/authenticate/direct", request, APPLICATION_JSON, null);
	}

	@Override
	protected String writeValueAsString(Object requestValue) throws HttpClientException {
		try {
			return objectMapper.writeValueAsString(requestValue);
		} catch (JsonProcessingException e) {
			throw new HttpClientException("Unable to serialize object", e);
		}
	}

	@Override
	protected ConnectorException nonSuccessResponseUsing(HttpResponse httpResponse) {
		try {
			return new ConnectorException(objectMapper.readValue(httpResponse.getOutput(), ErrorResponse.class),
					httpResponse.getHttpCode());
		} catch (IOException e) {
			return new ConnectorException(new ErrorResponse()
					.withMessageAs(httpResponse.getOutput()),
					httpResponse.getHttpCode());
		}
	}

	@Override
	protected <R> Converter<R> createConverter(Class<R> targetClass) {
		return httpResponse -> null;
	}
}
