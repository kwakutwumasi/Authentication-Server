package com.quakearts.auth.server.totp.edge.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import java.util.HashMap;
import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.edge.channel.DeviceConnection;
import com.quakearts.auth.server.totp.edge.channel.impl.DeviceConnectionServiceImpl;
import com.quakearts.auth.server.totp.edge.exception.CapacityExceededException;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.edge.test.runner.MainRunner;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

@RunWith(MainRunner.class)
public class DeviceConnectionServiceImplTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Inject
	private DeviceConnectionServiceImpl impl;
	
	@Test
	public void testRegisterUnRegisterConnectionAndSend() throws Exception {
		DeviceConnection connection = new TestDeviceConnection();
		impl.registerConnection(connection);
		Payload payload = new Payload();
		payload.setMessage(new HashMap<>());
		payload.getMessage().put("deviceId", "12345");
		payload.getMessage().put("test", "request");
		payload = impl.send(payload);
		assertThat(payload, is(notNullValue()));
		assertThat(payload.getMessage(), is(notNullValue()));
		assertThat(payload.getMessage().size(), is(1));
		assertThat(payload.getMessage().get("test"), is("response"));
		impl.unregisterConnection(connection);
		expectedException.expect(UnconnectedDeviceException.class);
		impl.send(payload);
	}

	@Test
	public void testMissingDeviceId() throws Exception {
		expectedException.expect(UnconnectedDeviceException.class);
		Payload payload = new Payload();
		payload.setMessage(new HashMap<>());
		impl.send(payload);
	}

	@Test
	public void testWithNonExistentDeviceID() throws Exception {
		expectedException.expect(UnconnectedDeviceException.class);
		Payload payload = new Payload();
		payload.setMessage(new HashMap<>());
		payload.getMessage().put("deviceId", "34567");
		payload.getMessage().put("test", "request");
		payload = impl.send(payload);
	}
	
	class TestDeviceConnection implements DeviceConnection {
		
		@Override
		public String getDeviceId() {
			return "12345";
		}

		@Override
		public void send(Payload payload) {
			assertThat(payload, is(notNullValue()));
			assertThat(payload.getMessage(), is(notNullValue()));
			assertThat(payload.getMessage().get("deviceId"), is("12345"));
			assertThat(payload.getMessage().get("test"), is("request"));
			assertThat(payload.getTimestamp(), is(notNullValue()));
		}

		@Override
		public void respond(Payload payload) throws CapacityExceededException {}

		@Override
		public Payload retrieve() {
			Payload payload = new Payload();
			payload.setMessage(new HashMap<>());
			payload.getMessage().put("test", "response");
			return payload;
		}
	}
}
