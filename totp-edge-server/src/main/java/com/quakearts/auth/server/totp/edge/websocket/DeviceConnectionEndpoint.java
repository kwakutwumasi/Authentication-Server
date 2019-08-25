package com.quakearts.auth.server.totp.edge.websocket;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnection;
import com.quakearts.auth.server.totp.edge.channel.DeviceConnectionService;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;
import com.quakearts.auth.server.totp.options.TOTPEdgeOptions;

@ApplicationScoped
@ServerEndpoint(value = "/device-connection/{deviceId}",
		decoders = {JSONConverter.class},
		encoders = {JSONConverter.class},
		configurator = CDIServerEndpointConfigurator.class)
public class DeviceConnectionEndpoint {
		
	@Inject
	private DeviceConnectionService connectionService;
	
	@Inject
	private TOTPEdgeOptions totpEdgeOptions;
	
	@OnOpen
	public void opened(Session session, @PathParam("deviceId") String deviceId){
		connectionService.registerConnection(new WebsocketSessionDeviceConnection(session, deviceId, 
				totpEdgeOptions.getPayloadQueueTimeout(), 
				totpEdgeOptions.getPayloadQueueSize()));
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
