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
		if(Boolean.parseBoolean(System.getProperty("totp.edge.server.connection.active","true"))){
			log.trace("Web Socket open request for deviceId {}", deviceId);
			AuthenticationRequest request = new AuthenticationRequest();
			request.setDeviceId(deviceId);
			request.setOtp(otp);
			try {
				client.authentication(request);
			} catch (IOException | HttpClientException e) {
				close(session, deviceId, "OTP Authentication failure. "+e.getMessage());
				return;
			}
			
			connectionService.registerConnection(new WebsocketSessionDeviceConnection(session, deviceId, 
					totpEdgeOptions.getPayloadQueueTimeout()));
		} else {
			close(session, deviceId, "Service not available");
		}
	}

	private void close(Session session, String deviceId, String reasonPhrase) {
		try {
			session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reasonPhrase));
		} catch (IOException e1) {
			log.error("Unable to send close response to deviceId with hashCode: {}. {}", deviceId.hashCode(), 
					e1);
		}
	}
	
	@OnClose
	public void closed(Session session){
		log.trace("Web Socket close request received");
		DeviceConnection deviceConnection = getDeviceConnection(session);
		if(deviceConnection!=null){
			connectionService.unregisterConnection(deviceConnection);
			log.trace("Web Socket closed for {}", deviceConnection.getDeviceId());
		}
	}

	@OnMessage
	public void received(Session session, Payload payload){
		log.trace("Web Socket message received");
		DeviceConnection deviceConnection = getDeviceConnection(session);
		if(deviceConnection!=null) {
			deviceConnection.respond(payload);
			log.trace("Web Socket message processed for {}", deviceConnection.getDeviceId());
		}
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
