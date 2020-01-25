package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.security.KeyStore;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.awaitility.Awaitility.*;
import static org.awaitility.Duration.*;

import javax.inject.Inject;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeTOTPOptions;
import com.quakearts.auth.server.totp.channel.impl.ConnectionManagerImpl;
import com.quakearts.auth.server.totp.exception.InvalidInputException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.options.TOTPOptions.PerformancePreferences;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class ConnectionManagerImplTest {

	private static final String TEST_MESSAGE = "test.message";
	@Inject
	private ConnectionManagerImpl connectionManagerImpl;
	private SSLContext context;
	
	@Test
	public void testInitSendEchoAndShutdown() throws Exception {
		AlternativeTOTPOptions.returnConnectionEchoInterval(1000l);
		connectionManagerImpl.init();
		try(Socket socket = createClientSocket(9001)){
			TestEchoClient client = new TestEchoClient(socket);
			await().atMost(TWO_SECONDS).until(client::testReceiveEcho);
			await().atMost(TWO_SECONDS).until(client::testReceiveEcho);
			CompletableFuture.runAsync(this::sendTestMessage);
			await().atMost(TWO_SECONDS).until(client::testReceiveMessage);
			await().atMost(TWO_SECONDS).until(()->{
				assertArrayEquals(response.messageBites, TEST_MESSAGE.getBytes());
				return true;
			});
		} finally {
			connectionManagerImpl.shutdown();
		}
	}
	
	@Test
	public void testInitWithAllOptionsMultipleConnectionsAndOneConnectionLost() throws Exception {
		AlternativeTOTPOptions.returnConnectionEchoInterval(1000l);
		AlternativeTOTPOptions.returnConnectionPort(9002);
		AlternativeTOTPOptions.returnConnectionReceiveBufferSize(1024);
		AlternativeTOTPOptions.returnConnectionReuseAddress(Boolean.FALSE);
		AlternativeTOTPOptions.returnPerformancePreferences(new PerformancePreferences(1, 1, 1024));
		
		connectionManagerImpl.init();
		try(Socket socket = createClientSocket(9002)){
			TestEchoClient client = new TestEchoClient(socket);
			await().atMost(TWO_SECONDS).until(client::testReceiveEcho);
		}
	
		try(Socket socket = createClientSocket(9002)){
			TestEchoClient client = new TestEchoClient(socket);
			await().atMost(TWO_SECONDS).until(client::testReceiveEcho);
		} finally {
			connectionManagerImpl.shutdown();
		}
	}

	@SuppressWarnings({ "rawtypes", "serial" })
	@Test
	public void testSendUnsolicitedMessage() throws Exception {
		AlternativeTOTPOptions.returnConnectionEchoInterval(100000l);
		AlternativeTOTPOptions.returnConnectionPort(9003);
		
		class Checker {
			boolean removed;
		}
		
		Checker check = new Checker();
		
		ConcurrentHashMap monitoredMap = new ConcurrentHashMap(){
			@Override
			public Object remove(Object key) {
				check.removed = (new Long(9223372036854775807l)).equals(key);
				return super.remove(key);
			}
		};
		Field field = ConnectionManagerImpl.class.getDeclaredField("callbackStore");
		field.setAccessible(true);
		field.set(connectionManagerImpl, monitoredMap);
		
		connectionManagerImpl.init();
		try(Socket socket = createClientSocket(9003)){
			OutputStream out = socket.getOutputStream();
			byte[] message = new byte[] {1, 4, 127, (byte)255, (byte)255, (byte)255, (byte)255, (byte)255, (byte)255, (byte)255, 't', 'e','s','t'};
			out.write(message);
			out.flush();
			await().atMost(TWO_SECONDS).until(()->check.removed);
		} finally {
			connectionManagerImpl.shutdown();
		}
	}
	
	@SuppressWarnings({ "rawtypes", "serial" })
	@Test
	public void testCleanOrphanedCallback() throws Exception {
		AlternativeTOTPOptions.returnConnectionEchoInterval(1300l);
		AlternativeTOTPOptions.returnConnectionPort(9004);
		AlternativeTOTPOptions.returnDeviceAuthenticationTimeout(100l);
		
		class Checker {
			Long value;
		}
		
		Checker check = new Checker();

		Consumer<byte[]> callback = bites->{
			check.value = 1l;
		};
		
		ConcurrentHashMap monitoredMap = new ConcurrentHashMap(){
			@Override
			public Object remove(Object key) {
				if(check.value == null) {
					check.value = 2l;
				}
				return super.remove(key);
			}
		};
		Field field = ConnectionManagerImpl.class.getDeclaredField("callbackStore");
		field.setAccessible(true);
		field.set(connectionManagerImpl, monitoredMap);
		
		connectionManagerImpl.init();
		try(Socket socket = createClientSocket(9004)){
			byte[] message = new byte[] {'t', 'e','s','t'};
			connectionManagerImpl.send(message, callback);
			await().atMost(TWO_SECONDS).until(()->Long.valueOf(2l).equals(check.value));
		} finally {
			connectionManagerImpl.shutdown();
		}
	}

	private Socket createClientSocket(int port) throws Exception {
		if(context == null) {
			KeyStore ks = KeyStore.getInstance("PKCS12");
			ks.load(Thread.currentThread().getContextClassLoader()
					.getResourceAsStream("totp.keystore"), "password".toCharArray());
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(ks, "password".toCharArray());
			
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(ks);
	
			context = SSLContext.getInstance("TLSv1.2");
			context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		}
		return context.getSocketFactory().createSocket("localhost", port);
	}
	
	class TestEchoClient {
		InputStream in;
		OutputStream out;
		
		public TestEchoClient(Socket socket) throws IOException{
			in = socket.getInputStream();
			out = socket.getOutputStream();
		}
		
		boolean testReceiveEcho() throws IOException {
			byte[] lengthHeader = new byte[2];
			int read = in.read(lengthHeader);
			
			assertThat(read, is(2));
			assertThat((lengthHeader[0]*8 + lengthHeader[1])&0x07ff, is(9));
			byte[] echobites = new byte[9];
			read = in.read(echobites);
			assertThat(read, is(9));
			assertThat(echobites[8], is((byte) 0));
			out.write(lengthHeader);
			out.write(echobites);
			return true;
		}
		
		boolean testReceiveMessage() throws IOException {
			byte[] lengthHeader = new byte[2];
			int read = in.read(lengthHeader);
			
			assertThat(read, is(2));
			assertThat((lengthHeader[0]*8 + lengthHeader[1])&0x07ff, is(20));
			byte[] messageBites = new byte[20];
			read = in.read(messageBites);
			assertThat(read, is(20));
			assertThat(new String(messageBites, 8, 12),is(TEST_MESSAGE));
			out.write(lengthHeader);
			out.write(messageBites);
			return true;
		}
	}

	class Response {
		byte[] messageBites;
	}
	Response response = new Response();

	private void sendTestMessage() {		
		try {
			connectionManagerImpl.send(TEST_MESSAGE.getBytes(), messageBites->{
				response.messageBites = messageBites;
			});
		} catch (TOTPException e) {
			fail(e.getMessage());
		}
	}
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testDeviceConnectionWithSizeGreaterThan() throws Exception {
		expectedException.expect(InvalidInputException.class);
		connectionManagerImpl.send(new byte[2040], bite->{});
	}
}
