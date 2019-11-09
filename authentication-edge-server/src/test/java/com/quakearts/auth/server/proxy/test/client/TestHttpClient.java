package com.quakearts.auth.server.proxy.test.client;

import java.io.IOException;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.proxy.client.exception.ConnectorException;
import com.quakearts.auth.server.proxy.client.model.ErrorResponse;
import com.quakearts.auth.server.proxy.client.model.TokenResponse;
import com.quakearts.rest.client.HttpObjectClient;
import com.quakearts.rest.client.HttpResponse;
import com.quakearts.rest.client.exception.HttpClientException;

@Singleton
@Alternative
public class TestHttpClient extends HttpObjectClient {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2968088214812254664L;
	private ObjectMapper objectMapper = new ObjectMapper();

	public TokenResponse authenticate(String alias, String application, String clientId, String credential) 
			throws IOException, HttpClientException {
		return executeGet("/authenticate/{0}/{1}/?clientId={2}&credential={3}",
				TokenResponse.class, alias, application, encode(clientId), encode(credential));
	}

	public void emptyAuthentication(String alias, String application) 
			throws IOException, HttpClientException{
		executeGet("/authenticate/{0}/{1}/", null, alias, application);
	}
	
	@Override
	protected ConnectorException nonSuccessResponseUsing(HttpResponse httpResponse) {
		try {
			return new ConnectorException(objectMapper.readValue(httpResponse.getOutput(), ErrorResponse.class),
					httpResponse.getHttpCode());
		} catch (IOException e) {
			return new ConnectorException(new ErrorResponse()
					.withCodeAs("deserialization-error")
					.addExplanation(httpResponse.getOutput()),
					httpResponse.getHttpCode());
		}
	}

	@Override
	protected String writeValueAsString(Object requestValue) throws HttpClientException {
		return null;
	}

	@Override
	protected <R> Converter<R> createConverter(Class<R> targetClass) {
		return httpResponse -> {
			try {
				return objectMapper.readValue(httpResponse.getOutputBytes(), targetClass);
			} catch (IOException e) {
				throw new HttpClientException("Unable to de-serialize response", e);
			}
		};
	}
}
