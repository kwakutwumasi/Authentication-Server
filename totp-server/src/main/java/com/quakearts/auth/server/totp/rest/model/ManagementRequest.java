package com.quakearts.auth.server.totp.rest.model;

public class ManagementRequest {
	private static final DeviceRequest[] DEFAULTREQUESTS = new DeviceRequest[0];
	
	private DeviceRequest[] requests = DEFAULTREQUESTS;
	
	public DeviceRequest[] getRequests() {
		return requests;
	}
	
	public void setRequests(DeviceRequest[] requests) {
		this.requests = requests;
	}
	
	private static final AuthorizationRequest DEFAULTAUTHORIZATIONREQUEST = new AuthorizationRequest();
	
	private AuthorizationRequest authorizationRequest = DEFAULTAUTHORIZATIONREQUEST;
	
	public AuthorizationRequest getAuthorizationRequest() {
		return authorizationRequest;
	}
	
	public void setAuthorizationRequest(AuthorizationRequest authorizationRequest) {
		if(authorizationRequest!=null)
			this.authorizationRequest = authorizationRequest;
	}
}
