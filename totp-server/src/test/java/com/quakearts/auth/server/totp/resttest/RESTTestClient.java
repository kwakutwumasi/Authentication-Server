package com.quakearts.auth.server.totp.resttest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.rest.model.ActivationRequest;
import com.quakearts.auth.server.totp.rest.model.AdministratorResponse;
import com.quakearts.auth.server.totp.rest.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.rest.model.CountResponse;
import com.quakearts.auth.server.totp.rest.model.DeviceResponse;
import com.quakearts.auth.server.totp.rest.model.ManagementRequest;
import com.quakearts.auth.server.totp.rest.model.ManagementResponse;
import com.quakearts.auth.server.totp.rest.model.ProvisioningResponse;
import com.quakearts.auth.server.totp.rest.model.TokenResponse;
import com.quakearts.rest.client.HttpClient;
import com.quakearts.rest.client.HttpResponse;
import com.quakearts.rest.client.HttpVerb;
import com.quakearts.rest.client.exception.HttpClientException;

import static java.text.MessageFormat.format;

public class RESTTestClient extends HttpClient {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8441758159666006784L;
	private String requestJWTToken;
	private static final String APPLICATION_JSON = "application/json";
	private ObjectMapper objectMapper = new ObjectMapper();
	
	private <T> T execute(String template, Object requestValue, String contentType, HttpVerb verb, Class<T> targetClass,
			Object... parameters) throws IOException, HttpClientException {
		return getHttpResponseUsing(template,
				requestValue != null ? objectMapper.writeValueAsString(requestValue) : null, contentType, verb,
				parameters).thenCoerceTo(targetClass);
	}
	
	private void executeWithNoResponse(String template, Object requestValue, String contentType, HttpVerb verb,
			Object... parameters) throws IOException, HttpClientException {
		getHttpResponseUsing(template,
				requestValue != null ? objectMapper.writeValueAsString(requestValue) : null, contentType, verb,
				parameters);
	}

	private HttpResponseBuilder getHttpResponseUsing(String template, String requestValue, String contentType,
			HttpVerb verb, Object... parameters) throws IOException, HttpClientException {
		HttpResponse httpResponse = sendAuthenticatedRequest(withTemplate(template, parameters), requestValue, verb,
				contentType);
		if (httpResponse.getHttpCode() < 200 || httpResponse.getHttpCode() > 299) {
			throw nonSuccessResponseUsing(httpResponse);
		}

		return new HttpResponseBuilder(httpResponse);
	}

	private String withTemplate(String template, Object... parameters) {
		return parameters.length > 0 ? format(template, parameters) : template;
	}

	private class HttpResponseBuilder {
		HttpResponse httpResponse;

		private HttpResponseBuilder(HttpResponse httpResponse) {
			this.httpResponse = httpResponse;
		}

		private <T> T thenCoerceTo(Class<T> targetClass) throws IOException {
			return objectMapper.readValue(httpResponse.getOutput(), targetClass);
		}

		private <T> T thenCoerceTo(TypeReference<T> typeReference) throws IOException {
			return objectMapper.readValue(httpResponse.getOutput(), typeReference);
		}
	}

	private HttpResponse sendAuthenticatedRequest(String file, String requestValue, HttpVerb method, String contentType)
			throws IOException, HttpClientException {
		Map<String, List<String>> authHeader = null;
		if(requestJWTToken!=null){
			authHeader = new HashMap<>();
			authHeader.put("Authorization", Arrays.asList("Bearer: "+requestJWTToken));
		}
		return sendRequest(file, requestValue, method, contentType, authHeader);
	}

	private String encode(String format) throws UnsupportedEncodingException {
		return URLEncoder.encode(format, "UTF-8");
	}

	private HttpClientException nonSuccessResponseUsing(HttpResponse httpResponse) {
		return new HttpClientException(
				"Unable to process request: " + httpResponse.getHttpCode() + "; " + httpResponse.getOutput());

	}

	public ProvisioningResponse provision(String deviceid) throws IOException, HttpClientException {
		return execute("/totp/provisioning/{0}", "", APPLICATION_JSON, HttpVerb.POST, ProvisioningResponse.class, encode(deviceid));
	}

	public void activate(String deviceid, ActivationRequest activationRequest) throws IOException, HttpClientException {
		executeWithNoResponse("/totp/provisioning/{0}", activationRequest, APPLICATION_JSON, HttpVerb.PUT, deviceid);
	}

	public void authenticate(AuthenticationRequest authenticateRequest) throws IOException, HttpClientException {
		executeWithNoResponse("/totp/authenticate", authenticateRequest, APPLICATION_JSON, HttpVerb.POST);
	}

	public void authenticateDirect(String deviceId) throws IOException, HttpClientException {
		executeWithNoResponse("/totp/authenticate/device/{0}", null, null, HttpVerb.GET, deviceId);
	}

	public TokenResponse login(AuthenticationRequest authorizationRequest) throws IOException, HttpClientException {
		TokenResponse response = execute("/totp/management-login", authorizationRequest, APPLICATION_JSON, HttpVerb.POST, TokenResponse.class);
		requestJWTToken = response.getToken();
		return response;
	}
	
	public ManagementResponse assignAliases(ManagementRequest managementRequest) throws IOException, HttpClientException {
		return execute("/totp/manage/assign-alias", managementRequest, APPLICATION_JSON, HttpVerb.POST, ManagementResponse.class);
	}

	public ManagementResponse unassignAliases(ManagementRequest managementRequest) throws IOException, HttpClientException {
		return execute("/totp/manage/unassign-alias", managementRequest, APPLICATION_JSON, HttpVerb.POST, ManagementResponse.class);
	}

	public ManagementResponse lock(ManagementRequest managementRequest) throws IOException, HttpClientException {
		return execute("/totp/manage/lock", managementRequest, APPLICATION_JSON, HttpVerb.POST, ManagementResponse.class);
	}

	public ManagementResponse unlock(ManagementRequest managementRequest) throws IOException, HttpClientException {
		return execute("/totp/manage/unlock", managementRequest, APPLICATION_JSON, HttpVerb.POST, ManagementResponse.class);
	}

	public ManagementResponse addAsAdmin(ManagementRequest managementRequest) throws IOException, HttpClientException {
		return execute("/totp/manage/add-as-admin", managementRequest, APPLICATION_JSON, HttpVerb.POST, ManagementResponse.class);
	}

	public ManagementResponse removeAsAdmin(ManagementRequest managementRequest) throws IOException, HttpClientException {
		return execute("/totp/manage/remove-as-admin", managementRequest, APPLICATION_JSON, HttpVerb.POST, ManagementResponse.class);		
	}

	public ManagementResponse deactivate(ManagementRequest managementRequest) throws IOException, HttpClientException {
		return execute("/totp/manage/deactivate", managementRequest, APPLICATION_JSON, HttpVerb.POST, ManagementResponse.class);		
	}

	public List<AdministratorResponse> listAdministrators() throws IOException, HttpClientException {
		return getHttpResponseUsing("/totp/manage/list-administrators",
				 null, null, HttpVerb.GET).thenCoerceTo(new TypeReference<List<AdministratorResponse>>() {});
	}

	public CountResponse countDevices() throws IOException, HttpClientException {
		return execute("/totp/manage/count-devices", null, null, HttpVerb.GET, CountResponse.class);				
	}

	public List<DeviceResponse> getDevices(Status status, long lastId, int maxRows) throws IOException, HttpClientException {
		return getHttpResponseUsing("/totp/manage/get-devices?status={0}&lastid={1}&maxrows={2}",
				 null, null, HttpVerb.GET,status, lastId, maxRows).thenCoerceTo(new TypeReference<List<DeviceResponse>>() {});
	}
	
	public List<DeviceResponse> getDevices() throws IOException, HttpClientException {
		return getHttpResponseUsing("/totp/manage/get-devices",
				 null, null, HttpVerb.GET).thenCoerceTo(new TypeReference<List<DeviceResponse>>() {});
	}

	public SyncResponse synchronize() throws IOException, HttpClientException {
		return execute("/totp/sync", null, null, HttpVerb.GET, SyncResponse.class);
	}

	public static class SyncResponse {
		private long time;
		public long getTime() {
			return time;
		}
		
		public void setTime(long time) {
			this.time = time;
		}
	}
}
