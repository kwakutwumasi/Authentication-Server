package com.quakearts.auth.server.totp.client;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.quakearts.auth.server.totp.client.exception.ConnectorException;
import com.quakearts.auth.server.totp.client.http.TOTPHttpClient;
import com.quakearts.auth.server.totp.client.http.TOTPHttpClientBuilder;
import com.quakearts.auth.server.totp.client.http.model.ActivationRequest;
import com.quakearts.auth.server.totp.client.http.model.ProvisioningResponse;
import com.quakearts.rest.client.exception.HttpClientException;

public class DeviceProvisioner {
	private static final DeviceProvisioner instance = new DeviceProvisioner();
	
	public static DeviceProvisioner getInstance() {
		return instance;
	}
	
	public Device provision(String deviceId, String alias) 
			throws IOException, HttpClientException, ConnectorException, GeneralSecurityException {
		TOTPHttpClient client = new TOTPHttpClientBuilder()
				.setURLAs(Options.getInstance().getTotpUrl())
				.thenBuild();
		
		ProvisioningResponse provisioningResponse = client.provision(deviceId);
		Device device = new Device(deviceId, 
				HexTool.hexAsByte(provisioningResponse.getSeed()), 
				provisioningResponse.getInitialCounter());
		
		ActivationRequest request = new ActivationRequest();
		request.setAlias(alias);
		request.setToken(device.generateOTP());
		client.activate(deviceId, request);
		
		return device;
	}
}
