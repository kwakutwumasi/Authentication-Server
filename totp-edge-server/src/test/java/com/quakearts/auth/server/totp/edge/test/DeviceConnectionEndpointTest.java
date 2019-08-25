package com.quakearts.auth.server.totp.edge.test;

import static org.junit.Assert.*;
import static org.awaitility.Awaitility.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.EncodeException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.awaitility.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnectionService;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.test.runner.MainRunner;
import com.quakearts.auth.server.totp.edge.websocket.DeviceConnectionEndpoint;
import com.quakearts.auth.server.totp.edge.websocket.JSONConverter;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;

@RunWith(MainRunner.class)
public class DeviceConnectionEndpointTest {

	@Inject
	private DeviceConnectionService service;
	
	@Test
	public void testEndpoint() 
			throws Exception {
		ContainerProvider.getWebSocketContainer()
			.connectToServer(new Endpoint() {
				@Override
				public void onOpen(Session session, EndpointConfig config) {
					session.addMessageHandler(new MessageHandler.Whole<Payload>() {
						@Override
						public void onMessage(Payload message) {
							Payload response = new Payload();
							response.setId(message.getId());
							response.setMessage(new HashMap<>());
							response.getMessage().put("test", "response");
							response.getMessage().put("request-value", message.getMessage().get("test"));
							try {
								session.getBasicRemote().sendObject(response);
							} catch (IOException | EncodeException e) {}
							try {
								long start = System.currentTimeMillis();
								while (System.currentTimeMillis()-start<1000) {}
								session.getBasicRemote().sendObject(response);
								response = new Payload();
								response.setMessage(new HashMap<>());
								response.getMessage().put("test", "response");
								session.getBasicRemote().sendObject(response);
								session.close();
							} catch (IOException | EncodeException e) {}
						}
					});
				}
			}, ClientEndpointConfig.Builder.create()
					.decoders(Arrays.asList(JSONConverter.class))
					.encoders(Arrays.asList(JSONConverter.class))
					.build(), new URI("ws://localhost:8082/device-connection/testdevice1"));
		Payload payload = new Payload();
		payload.setMessage(new HashMap<>());
		payload.getMessage().put("test", "request");
		payload.getMessage().put("deviceId", "testdevice1");
		class Holder {
			Payload response;
		}
		
		Holder holder = new Holder();
		
		await().atMost(Duration.ONE_SECOND).until(()->{
			service.send(payload, payloadResponse->{
				holder.response = payloadResponse;
			});
			return true;
		});
		
		await().atMost(Duration.TWO_SECONDS).until(()->{			
			assertThat(holder.response, is(notNullValue()));
			assertThat(holder.response.getMessage(), is(notNullValue()));
			assertThat(holder.response.getMessage().get("test"), is("response"));
			assertThat(holder.response.getMessage().get("request-value"), is("request"));
			return true;
		});
		
		Payload rejectPayload = new Payload();
		rejectPayload.setMessage(new HashMap<>());
		rejectPayload.getMessage().put("test", "request");
		rejectPayload.getMessage().put("deviceId", "testdevice1");
		await().atMost(Duration.FIVE_SECONDS).until(()->{
			try {				
				service.send(rejectPayload, response->{});
			} catch (UnconnectedDeviceException e) {
				return true;
			}
			return false;
		});
	}
	
	
	@Test
	public void testClosedWithNoConnectionInSession() throws Exception {
		Map<String, Object> props = new HashMap<>();
		Session session = MockingProxyBuilder
				.createMockingInvocationHandlerFor(Session.class)
				.mock("getUserProperties").withEmptyMethod(()->{
					return props;
				}).thenBuild();
		
		new DeviceConnectionEndpoint().closed(session);
	}
	
	@Test
	public void testReceivedWithNoDeviceConnectionInSession() throws Exception {
		Map<String, Object> props = new HashMap<>();
		Session session = MockingProxyBuilder
				.createMockingInvocationHandlerFor(Session.class)
				.mock("getUserProperties").withEmptyMethod(()->{
					return props;
				}).thenBuild();
		CDI.current().select(DeviceConnectionEndpoint.class)
		.get().received(session, new Payload());
	}
}
