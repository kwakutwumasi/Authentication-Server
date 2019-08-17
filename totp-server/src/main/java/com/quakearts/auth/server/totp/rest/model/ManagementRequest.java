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
	
	private static final AuthenticationRequest DEFAULTAUTHORIZATIONREQUEST = new AuthenticationRequest();
	
	private AuthenticationRequest authorizationRequest = DEFAULTAUTHORIZATIONREQUEST;
	
	public AuthenticationRequest getAuthorizationRequest() {
		return authorizationRequest;
	}
	
	public void setAuthorizationRequest(AuthenticationRequest authorizationRequest) {
		if(authorizationRequest!=null)
			this.authorizationRequest = authorizationRequest;
	}
}
