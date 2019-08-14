package com.quakearts.auth.server.totp.provisioning.client;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.enterprise.inject.Any;
import javax.inject.Singleton;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.totp.provisioning.client.exception.ConnectorException;
import com.quakearts.auth.server.totp.provisioning.client.model.ErrorResponse;
import com.quakearts.auth.server.totp.provisioning.client.model.TokenResponse;
import com.quakearts.rest.client.HttpClient;
import com.quakearts.rest.client.HttpResponse;
import com.quakearts.rest.client.HttpVerb;
import com.quakearts.rest.client.exception.HttpClientException;

@Any
@Singleton
public class AuthenticationServerHttpClient extends HttpClient {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2968088214812254664L;
	static final String APPLICATION_JSON = "application/json";
	private ObjectMapper objectMapper = new ObjectMapper();
	protected String file;

	public TokenResponse authenticate(String alias, String application, String clientId, String credential) throws IOException, HttpClientException, ConnectorException {
		return getHttpResponseUsing(file+"/authenticate/{0}/{1}/?clientId={2}&credential{3}",
				null, null , HttpVerb.POST,
				alias, application, encode(clientId), encode(credential)).thenCoerceTo(TokenResponse.class);
	}
	
	private HttpResponseBuilder getHttpResponseUsing(String template, Object requestValue, String contentType,
			HttpVerb verb, Object... parameters) throws IOException, HttpClientException, ConnectorException {
		HttpResponse httpResponse = sendRequest(withTemplate(template, parameters), 
				requestValue != null ? stringify(requestValue) : null, verb, contentType);
		if (httpResponse.getHttpCode() < 200 || httpResponse.getHttpCode() > 299) {
			throw nonSuccessResponseUsing(httpResponse);
		}

		return new HttpResponseBuilder(httpResponse);
	}

	private String stringify(Object requestValue) throws JsonProcessingException {
		return requestValue instanceof String?(String)requestValue:objectMapper.writeValueAsString(requestValue);
	}

	private String withTemplate(String template, Object... parameters) throws UnsupportedEncodingException {
		return parameters.length > 0 ? encode(format(template, parameters)) : template;
	}

	private String encode(String format) throws UnsupportedEncodingException {
		return format!=null? URLEncoder.encode(format, "UTF-8"):"";
	}

	private class HttpResponseBuilder {
		HttpResponse httpResponse;

		private HttpResponseBuilder(HttpResponse httpResponse) {
			this.httpResponse = httpResponse;
		}

		private <T> T thenCoerceTo(Class<T> targetClass) throws IOException {
			return objectMapper.readValue(httpResponse.getOutput(), targetClass);
		}
	}

	private ConnectorException nonSuccessResponseUsing(HttpResponse httpResponse) throws JsonParseException, JsonMappingException, IOException {
		return new ConnectorException(objectMapper.readValue(httpResponse.getOutput(), ErrorResponse.class),
				httpResponse.getHttpCode());
	}
}
