package com.quakearts.auth.server.totp.edge.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.*;

import javax.inject.Inject;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.edge.channel.impl.TOTPServerConnectionImpl;
import com.quakearts.auth.server.totp.edge.test.alternative.AlternativeTOTPServerMessageHandler;
import com.quakearts.auth.server.totp.edge.test.runner.MainRunner;
import com.quakearts.auth.server.totp.options.TOTPEdgeOptions;
import com.quakearts.webapp.security.jwt.exception.JWTException;

@RunWith(MainRunner.class)
public class TOTPServerConnectionImplTest {

	@Inject
	private TOTPServerConnectionImpl totpServerConnectionImpl;

	@Inject
	private TOTPEdgeOptions totpEdgeOptions;

	private SSLContext context;
	
	@Test
	public void testInitAndShutdown() throws Exception {
		AlternativeTOTPServerMessageHandler
			.returnBytes(bites->{
				assertThat(new String(bites), is("testMessage"));
				return "testResponse".getBytes();
			});
		Future<Boolean> serverProcess = CompletableFuture
				.supplyAsync(this::createServerAndSendRequest);
		totpServerConnectionImpl.init();
		assertThat(serverProcess.get(5,TimeUnit.SECONDS), is(true));
	}
	
	@Test
	public void testResponseWithJWTException() throws Exception {
		AlternativeTOTPServerMessageHandler
			.returnBytes(bites->{
				throw new JWTException("");
			});
		Future<Boolean> serverProcess = CompletableFuture
				.supplyAsync(this::createServerAndTestException);
		totpServerConnectionImpl.init();
		assertThat(serverProcess.get(5,TimeUnit.SECONDS), is(true));
	}

	private boolean createServerAndSendRequest() {
		try {
			SSLContext context = createSSLContext();
			
			try(ServerSocket socket = context.getServerSocketFactory()
					.createServerSocket(9001)){
				Socket clientSocket = socket.accept();
				OutputStream out = clientSocket.getOutputStream();
				InputStream in = clientSocket.getInputStream();
				
				byte[] bites = "testMessage".getBytes();
				byte[] lengthHeader = new byte[2];				
				lengthHeader[0] = (byte) (bites.length / 8);
				lengthHeader[1] = (byte) (bites.length % 8);
				out.write(lengthHeader);
				out.write(bites);
				assertThat(in.read(lengthHeader), is(2));
				int length = (lengthHeader[0]*8 + lengthHeader[1])&0x07ff;
				bites = new byte[length];
				assertThat(in.read(bites), is(length));
				assertThat(new String(bites), is("testResponse"));
			}
			totpServerConnectionImpl.shutdown();
			Field socketField = TOTPServerConnectionImpl.class
					.getDeclaredField("socket");
			socketField.setAccessible(true);
			assertThat(((Socket)socketField.get(totpServerConnectionImpl))
						.isClosed(), 
					is(true));
			Field runningField = TOTPServerConnectionImpl.class
					.getDeclaredField("running");
			runningField.setAccessible(true);
			assertThat(runningField.get(totpServerConnectionImpl),
				is(false));
		} catch (Exception e) {
			throw new AssertionError(e);
		}
		return true;
	}

	private boolean createServerAndTestException() {
		try {
			SSLContext context = createSSLContext();
		
			try(ServerSocket socket = context.getServerSocketFactory()
					.createServerSocket(9001)){
				Socket clientSocket = socket.accept();
				OutputStream out = clientSocket.getOutputStream();
				InputStream in = clientSocket.getInputStream();
				
				byte[] bites = "testMessage".getBytes();
				byte[] lengthHeader = new byte[2];				
				lengthHeader[0] = (byte) (bites.length / 8);
				lengthHeader[1] = (byte) (bites.length % 8);
				out.write(lengthHeader);
				out.write(bites);
				assertThat(in.read(lengthHeader), is(2));
				int length = (lengthHeader[0]*8 + lengthHeader[1])&0x07ff;
				bites = new byte[length];
				assertThat(in.read(bites), is(length));
				assertArrayEquals(bites, new byte[] {(byte)255});
			}
			totpServerConnectionImpl.shutdown();
		} catch (Exception e) {
			throw new AssertionError(e);
		}
		return true;
	}
	
	private SSLContext createSSLContext() throws KeyStoreException, IOException, NoSuchAlgorithmException,
			CertificateException, UnrecoverableKeyException, KeyManagementException {
		if(context!=null)
			return context;
		
		KeyStore ks = KeyStore.getInstance(totpEdgeOptions.getKeystoreType());
		String keystorePassword = totpEdgeOptions.getKeystorePassword();
		ks.load(Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(totpEdgeOptions.getKeystore()), keystorePassword.toCharArray());
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		String keyPassword = totpEdgeOptions.getKeyPassword();		
		keyManagerFactory.init(ks, keyPassword.toCharArray());
		
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(ks);
		
		context = SSLContext.getInstance(totpEdgeOptions.getSslInstance());
		context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		return context;
	}

}
