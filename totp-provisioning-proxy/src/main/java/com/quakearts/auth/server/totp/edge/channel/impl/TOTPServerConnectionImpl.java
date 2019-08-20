package com.quakearts.auth.server.totp.edge.channel.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.edge.channel.TOTPServerConnection;
import com.quakearts.auth.server.totp.edge.channel.TOTPServerMessageHandler;
import com.quakearts.auth.server.totp.options.TOTPEdgeOptions;
import com.quakearts.webapp.security.jwt.exception.JWTException;

@Singleton
public class TOTPServerConnectionImpl implements TOTPServerConnection {

	private static Logger log = LoggerFactory.getLogger(TOTPServerConnection.class);
	private static final byte[] NORESPONSE = new byte[]{(byte) 255};
	
	@Inject
	private TOTPEdgeOptions totpEdgeOptions;
	@Inject
	private TOTPServerMessageHandler totpServerMessageHandler;
	private SSLContext context;
	private boolean running;
	private Socket socket;
	
	@Override
	public void init() 
			throws IOException, KeyStoreException, NoSuchAlgorithmException, 
			CertificateException, UnrecoverableKeyException, KeyManagementException {
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
		new Thread(this::runClient).start();
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
	}

	private void runClient() {
		setRunning(true);
		while (isRunning()) {
			try {
				socket = context.getSocketFactory().createSocket(totpEdgeOptions.getTotpServerIp(), 
						totpEdgeOptions.getTotpServerPort());
				listen();
			} catch (IOException e) {
				log.error("Error processing TOTP server messages", e);
			} finally {
				close();
			}
		}
	}
	
	private void listen() throws IOException {
		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();
		while (isRunning()) {
			byte[] lengthHeader = new byte[2];
			int read = in.read(lengthHeader);
			assert(read==2);
			byte[] message = new byte[getLength(lengthHeader)];
			read = in.read(message);
			assert(read==message.length);
			byte[] response;
			try {
				response = totpServerMessageHandler
						.handle(message);
			} catch (JWTException e) {
				response = NORESPONSE;
			}
			lengthHeader = getLengthHeader(response);
			out.write(lengthHeader);
			out.write(response);
		}
	}

	private int getLength(byte[] lengthHeader) {
		return (lengthHeader[0]*8 + lengthHeader[1])&0x07ff;
	}

	private byte[] getLengthHeader(byte[] bites) {
		byte[] lengthHeader = new byte[2];
		lengthHeader[0] = (byte) (bites.length / 8);
		lengthHeader[1] = (byte) (bites.length % 8);
		
		return lengthHeader;
	}
	
	private synchronized void setRunning(boolean running) {
		this.running = running;
	}
	
	private synchronized boolean isRunning() {
		return running;
	}

	@Override
	public void shutdown() {
		if(isRunning()) {
			setRunning(false);
			close();
		}
	}

	private void close() {
		try {
			if(!socket.isClosed())
				socket.close();
		} catch (IOException e) {
			log.error("Error closing TOTP client socket", e);
		}
	}
}
