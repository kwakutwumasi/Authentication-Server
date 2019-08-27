package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeConnectionManager;
import com.quakearts.auth.server.totp.alternatives.AlternativeJWTGenerator;
import com.quakearts.auth.server.totp.channel.impl.DeviceConnectionChannelImpl;
import com.quakearts.auth.server.totp.exception.MessageGenerationException;
import com.quakearts.auth.server.totp.generator.impl.JWTGeneratorImpl;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class DeviceConnectionChannelImplTest {

	@Inject
	private DeviceConnectionChannelImpl impl;
	
	@Inject
	private JWTGeneratorImpl jwtGenerator;
		
	@Test
	public void testSendMessageWith1ByteResponse() throws Exception {
		AlternativeConnectionManager.run(bite->new byte[] {(byte)255});		
		Map<String, String> message = new HashMap<>();
		message.put("ignore", "this");
		impl.sendMessage(message, response->{
			assertThat(response, is(notNullValue()));
			assertThat(response.size(), is(1));
			assertThat(response.containsKey("error"), is(true));
			assertThat(response.get("error"), is("A connection has not been registered, or it may have been terminated by an error"));
		});
	}

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testSendMessageWithNoSuchAlgorithmExceptionOnGenerate() throws Exception {
		expectedException.expect(MessageGenerationException.class);
		expectedException.expect(isCausedBy(NoSuchAlgorithmException.class));
		AlternativeJWTGenerator.throwGenerateError(new NoSuchAlgorithmException());
		Map<String, String> message = new HashMap<>();
		message.put("ignore", "this");
		impl.sendMessage(message, response->{});
	}
	
	@Test
	public void testSendMessageWithURISyntaxExceptionOnGenerate() throws Exception {
		expectedException.expect(MessageGenerationException.class);
		expectedException.expect(isCausedBy(URISyntaxException.class));
		AlternativeJWTGenerator.throwGenerateError(new URISyntaxException("",""));
		Map<String, String> message = new HashMap<>();
		message.put("ignore", "this");
		impl.sendMessage(message, response->{});
	}
	
	@Test
	public void testSendMessageWithJWTExceptionOnGenerate() throws Exception {
		expectedException.expect(MessageGenerationException.class);
		expectedException.expect(isCausedBy(JWTException.class));
		AlternativeJWTGenerator.throwGenerateError(new JWTException(""));
		Map<String, String> message = new HashMap<>();
		message.put("ignore", "this");
		impl.sendMessage(message, response->{});
	}
	
	@Test
	public void testSendMessageWithNoSuchAlgorithmExceptionOnRespond() throws Exception {
		Map<String, String> message = new HashMap<>();
		message.put("ignore", "this");
		AlternativeConnectionManager.run(bite-> {
			try {
				return jwtGenerator.generateJWT(message).getBytes();
			} catch (NoSuchAlgorithmException | JWTException | URISyntaxException e) {
				throw new IllegalStateException(e);
			}
		});		
		expectedException.expect(MessageGenerationException.class);
		expectedException.expect(isCausedBy(NoSuchAlgorithmException.class));
		AlternativeJWTGenerator.throwVerifyError(new NoSuchAlgorithmException());
		impl.sendMessage(message, response->{});
	}
	
	@Test
	public void testSendMessageWithURISyntaxExceptionOnRespond() throws Exception {
		Map<String, String> message = new HashMap<>();
		message.put("ignore", "this");
		AlternativeConnectionManager.run(bite-> {
			try {
				return jwtGenerator.generateJWT(message).getBytes();
			} catch (NoSuchAlgorithmException | JWTException | URISyntaxException e) {
				throw new IllegalStateException(e);
			}
		});	
		expectedException.expect(MessageGenerationException.class);
		expectedException.expect(isCausedBy(URISyntaxException.class));
		AlternativeJWTGenerator.throwVerifyError(new URISyntaxException("",""));
		impl.sendMessage(message, response->{});
	}
	
	@Test
	public void testSendMessageWithJWTExceptionOnRespond() throws Exception {
		Map<String, String> message = new HashMap<>();
		message.put("ignore", "this");
		AlternativeConnectionManager.run(bite-> {
			try {
				return jwtGenerator.generateJWT(message).getBytes();
			} catch (NoSuchAlgorithmException | JWTException | URISyntaxException e) {
				throw new IllegalStateException(e);
			}
		});	
		expectedException.expect(MessageGenerationException.class);
		expectedException.expect(isCausedBy(JWTException.class));
		AlternativeJWTGenerator.throwVerifyError(new JWTException(""));
		impl.sendMessage(message, response->{});
	}

	private Matcher<?> isCausedBy(Class<?> clazz) {
		return new BaseMatcher<Object>() {
	
			@Override
			public boolean matches(Object item) {
				return item != null && item instanceof Exception
						&& ((Exception) item).getCause() != null
						&& clazz.isAssignableFrom(((Exception) item).getCause().getClass());
			}
	
			@Override
			public void describeTo(Description description) {
				description.appendText(" with a cause of "+clazz.getName());
			}
		};
	}
	
}
