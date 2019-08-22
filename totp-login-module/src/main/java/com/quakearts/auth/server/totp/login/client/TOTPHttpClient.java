package com.quakearts.auth.server.totp.login.client;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.net.URLEncoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.totp.login.client.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.login.client.model.ErrorResponse;
import com.quakearts.auth.server.totp.login.exception.ConnectorException;
import com.quakearts.rest.client.HttpClient;
import com.quakearts.rest.client.HttpResponse;
import com.quakearts.rest.client.HttpVerb;
import com.quakearts.rest.client.exception.HttpClientException;

public class TOTPHttpClient extends HttpClient {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2968088214812254664L;
	static final String APPLICATION_JSON = "application/json";
	private ObjectMapper objectMapper = new ObjectMapper();
	protected String file;
	
	public void authentication(AuthenticationRequest request) 
			throws IOException, HttpClientException, ConnectorException {
		getHttpResponseUsing(file+"/authenticate",
				request, APPLICATION_JSON , HttpVerb.POST);
	}
		
	public void authenticationDirect(String deviceId) 
			throws IOException, HttpClientException, ConnectorException {
		getHttpResponseUsing(file+"/authenticate/device/{0}",
				null, null, HttpVerb.GET, URLEncoder.encode(deviceId,"UTF-8"));
	}
	
	private void getHttpResponseUsing(String template, Object requestValue, String contentType,
			HttpVerb verb, Object... parameters) throws IOException, HttpClientException, ConnectorException {
		HttpResponse httpResponse = sendRequest(withTemplate(template, parameters), 
				stringify(requestValue), verb, contentType);
		if (httpResponse.getHttpCode() > 299) {
			throw nonSuccessResponseUsing(httpResponse);
		}
	}

	private String stringify(Object requestValue) throws JsonProcessingException {
		return requestValue != null?objectMapper.writeValueAsString(requestValue):null;
	}

	private String withTemplate(String template, Object... parameters) {
		return parameters.length > 0 ? format(template, parameters) : template;
	}

	private ConnectorException nonSuccessResponseUsing(HttpResponse httpResponse) throws IOException {
		return new ConnectorException(objectMapper.readValue(httpResponse.getOutput(), ErrorResponse.class),
				httpResponse.getHttpCode());
	}
}
