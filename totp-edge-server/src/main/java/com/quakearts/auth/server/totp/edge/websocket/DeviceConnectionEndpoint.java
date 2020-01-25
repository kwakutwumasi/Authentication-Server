package com.quakearts.auth.server.totp.edge.websocket;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnection;
import com.quakearts.auth.server.totp.edge.channel.DeviceConnectionService;
import com.quakearts.auth.server.totp.edge.client.TOTPServerHttpClient;
import com.quakearts.auth.server.totp.edge.client.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;
import com.quakearts.auth.server.totp.options.TOTPEdgeOptions;
import com.quakearts.rest.client.exception.HttpClientException;

@ApplicationScoped
@ServerEndpoint(value = "/device-connection/{deviceId}/{otp}",
		decoders = {JSONConverter.class},
		encoders = {JSONConverter.class},
		configurator = CDIServerEndpointConfigurator.class)
public class DeviceConnectionEndpoint {
		
	@Inject
	private DeviceConnectionService connectionService;
	
	@Inject
	private TOTPEdgeOptions totpEdgeOptions;
	
	@Inject
	private TOTPServerHttpClient client;
	
	private static final Logger log = LoggerFactory.getLogger(DeviceConnectionEndpoint.class);
	
	@OnOpen
	public void opened(Session session, @PathParam("deviceId") String deviceId, 
			@PathParam("otp") String otp){
		AuthenticationRequest request = new AuthenticationRequest();
		request.setDeviceId(deviceId);
		request.setOtp(otp);
		try {
			client.authentication(request);
		} catch (IOException | HttpClientException e) {
			try {
				session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "OTP Authentication failure. "+e.getMessage()));
			} catch (IOException e1) {
				log.error("Unable to send close response to "+deviceId, e1);
				return;
			}
		}
		
		connectionService.registerConnection(new WebsocketSessionDeviceConnection(session, deviceId, 
				totpEdgeOptions.getPayloadQueueTimeout()));
	}
	
	@OnClose
	public void closed(Session session){
		DeviceConnection deviceConnection = getDeviceConnection(session);
		if(deviceConnection!=null)
			connectionService.unregisterConnection(deviceConnection);
	}

	@OnMessage
	public void received(Session session, Payload payload){
		DeviceConnection deviceConnection = getDeviceConnection(session);
		if(deviceConnection!=null)
			deviceConnection.respond(payload);
	}

	private DeviceConnection getDeviceConnection(Session session) {
		DeviceConnection deviceConnection = null;
		if(session.getUserProperties().containsKey(WebsocketSessionDeviceConnection.DEVICE_CONNECTION)) {
			deviceConnection = (DeviceConnection) session.getUserProperties()
					.get(WebsocketSessionDeviceConnection.DEVICE_CONNECTION);
		}
		return deviceConnection;
	}
	
}
