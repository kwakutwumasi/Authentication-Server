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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnection;
import com.quakearts.auth.server.totp.edge.channel.DeviceConnectionService;
import com.quakearts.auth.server.totp.edge.exception.CapacityExceededException;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.test.runner.MainRunner;
import com.quakearts.auth.server.totp.edge.websocket.DeviceConnectionEndpoint;
import com.quakearts.auth.server.totp.edge.websocket.JSONConverter;
import com.quakearts.auth.server.totp.edge.websocket.WebsocketSessionDeviceConnection;
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
							response.setMessage(new HashMap<>());
							response.getMessage().put("test", "response");
							response.getMessage().put("request-value", message.getMessage().get("test"));
							try {
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
		await().atMost(Duration.TWO_SECONDS).until(()->{
			Payload payload = new Payload();
			payload.setMessage(new HashMap<>());
			payload.getMessage().put("test", "request");
			payload.getMessage().put("deviceId", "testdevice1");
			Payload response = service.send(payload);
			assertThat(response, is(notNullValue()));
			assertThat(response.getMessage(), is(notNullValue()));
			assertThat(response.getMessage().get("test"), is("response"));
			assertThat(response.getMessage().get("request-value"), is("request"));
			return true;
		});
		
		await().atMost(Duration.FIVE_SECONDS).until(()->{
			Payload payload = new Payload();
			payload.setMessage(new HashMap<>());
			payload.getMessage().put("test", "request");
			payload.getMessage().put("deviceId", "testdevice1");
			try {				
				service.send(payload);
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
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testReceivedWithCapacityExceededException() throws Exception {
		DeviceConnection connection = MockingProxyBuilder
				.createMockingInvocationHandlerFor(DeviceConnection.class)
				.mock("respond").with((arguments)->{
					throw new CapacityExceededException();
				})
				.thenBuild();
		Map<String, Object> props = new HashMap<>();
		props.put(WebsocketSessionDeviceConnection.DEVICE_CONNECTION, connection);		
		Session session = MockingProxyBuilder
				.createMockingInvocationHandlerFor(Session.class)
				.mock("getUserProperties").withEmptyMethod(()->{
					return props;
				}).thenBuild();
		CDI.current().select(DeviceConnectionEndpoint.class)
		.get().received(session, new Payload());
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
	
	@Test
	public void testRespond() throws Exception {
		Map<String, Object> props = new HashMap<>();
		Session session = MockingProxyBuilder
				.createMockingInvocationHandlerFor(Session.class)
				.mock("getUserProperties").withEmptyMethod(()->{
					return props;
				}).thenBuild();
		
		DeviceConnection connection = new WebsocketSessionDeviceConnection(session, "testDevice2", 100, 1);
		
		Payload payload = new Payload();
		payload.setTimestamp(System.currentTimeMillis()-10000l);
		
		connection.respond(payload);
		expectedException.expect(CapacityExceededException.class);
		connection.respond(new Payload());
		connection.respond(new Payload());
	}

}
