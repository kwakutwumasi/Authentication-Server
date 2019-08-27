package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeConnectionManager;
import com.quakearts.auth.server.totp.channel.impl.DeviceConnectionChannelImpl;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class DeviceConnectionChannelImplTest {

	@Inject
	private DeviceConnectionChannelImpl impl;
		
	@Test
	public void testSendMessageWith1ByteResponse() throws Exception {
		AlternativeConnectionManager.run(bite->new byte[] {(byte)255});
		
		Map<String, String> message = new HashMap<>();
		message.put("ignore", "this");
		impl.sendMessage(message, response->{
			assertThat(response, is(notNullValue()));
			assertThat(response.size(), is(1));
			assertThat(response.containsKey("error"), is(true));
			assertThat(response.get("error"), is("Not Connected"));
		});
	}

}
