package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.util.concurrent.CompletableFuture;

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
import com.quakearts.auth.server.totp.channel.DeviceConnection;
import com.quakearts.auth.server.totp.channel.impl.ConnectionManagerImpl;
import com.quakearts.auth.server.totp.exception.InvalidInputException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.options.TOTPOptions.PerformancePreferences;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class ConnectionManagerImplTest {

	private static final String TEST_MESSAGE = "test.message";
	@Inject
	private ConnectionManagerImpl connectionManagerImpl;
	@Inject
	private ConnectionManagerImpl connectionManagerImpl2;
	private SSLContext context;
	
	@Test
	public void testInitSendEchoAndShutdown() throws Exception {
		AlternativeTOTPOptions.returnConnectionEchoInterval(1000l);
		connectionManagerImpl.init();
		try(Socket socket = createClientSocket(9001)){
			TestEchoClient client = new TestEchoClient(socket);
			await().atMost(ONE_SECOND).until(client::testReceiveEcho);
			await().atMost(TWO_SECONDS).until(client::testReceiveEcho);
			CompletableFuture.runAsync(this::sendTestMessage);
			await().atMost(FIVE_SECONDS).until(client::testReceiveMessage);

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
			assertThat((lengthHeader[0]*8 + lengthHeader[1])&0x07ff, is(1));
			byte[] echobites = new byte[]{1};
			read = in.read(echobites);
			assertThat(read, is(1));
			assertThat(echobites[0], is((byte) 0));
			out.write(lengthHeader);
			out.write(echobites);
			return true;
		}
		
		boolean testReceiveMessage() throws IOException {
			byte[] lengthHeader = new byte[2];
			int read = in.read(lengthHeader);
			
			assertThat(read, is(2));
			assertThat((lengthHeader[0]*8 + lengthHeader[1])&0x07ff, is(12));
			byte[] messageBites = new byte[12];
			read = in.read(messageBites);
			assertThat(read, is(12));
			assertArrayEquals(messageBites, TEST_MESSAGE.getBytes());
			out.write(lengthHeader);
			out.write(messageBites);
			return true;
		}
	}

	private void sendTestMessage() {
		try {
			byte[] messageBites = connectionManagerImpl.send(TEST_MESSAGE.getBytes());
			assertArrayEquals(messageBites, TEST_MESSAGE.getBytes());
		} catch (UnconnectedDeviceException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testInitWithAllOptionsMultipleConnectionsAndOneConnectionLost() throws Exception {
		AlternativeTOTPOptions.returnConnectionEchoInterval(1000l);
		AlternativeTOTPOptions.returnConnectionPort(9002);
		AlternativeTOTPOptions.returnConnectionReceiveBufferSize(1024);
		AlternativeTOTPOptions.returnConnectionReuseAddress(Boolean.FALSE);
		AlternativeTOTPOptions.returnPerformancePreferences(new PerformancePreferences(1, 1, 1024));
		
		connectionManagerImpl2.init();
		try(Socket socket = createClientSocket(9002)){
			TestEchoClient client = new TestEchoClient(socket);
			await().atMost(ONE_SECOND).until(client::testReceiveEcho);
		}

		try(Socket socket = createClientSocket(9002)){
			TestEchoClient client = new TestEchoClient(socket);
			await().atMost(ONE_SECOND).until(client::testReceiveEcho);
			CompletableFuture.runAsync(this::sendTestMessage);
			await().atMost(FIVE_SECONDS).until(client::testReceiveMessage);
		} finally {
			connectionManagerImpl.shutdown();
		}
	}

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	public void testDeviceConnectionWithSizeGreaterThan() throws Exception {
		expectedException.expect(InvalidInputException.class);
		DeviceConnection deviceConnection = new DeviceConnection(null);
		deviceConnection.send(new byte[2048]);
	}
}
