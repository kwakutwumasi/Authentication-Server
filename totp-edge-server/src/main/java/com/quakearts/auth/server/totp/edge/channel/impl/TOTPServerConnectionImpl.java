package com.quakearts.auth.server.totp.edge.channel.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.edge.channel.Message;
import com.quakearts.auth.server.totp.edge.channel.TOTPServerConnection;
import com.quakearts.auth.server.totp.edge.channel.TOTPServerMessageHandler;
import com.quakearts.auth.server.totp.edge.exception.UnconnectedDeviceException;
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
		log.debug("Setting up connection...");
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
		new Thread(this::runClient,"IO-"+totpEdgeOptions.getTotpServerIp()+":"
				+totpEdgeOptions.getTotpServerPort()).start();
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
		log.debug("Set up complete");
	}

	private void runClient() {
		log.debug("Running client connection...");
		setRunning(true);
		while (isRunning()) {
			log.debug("Connecting...");
			try {
				socket = context.getSocketFactory().createSocket(totpEdgeOptions.getTotpServerIp(), 
						totpEdgeOptions.getTotpServerPort());
				socket.setSoTimeout(totpEdgeOptions.getTotpServerReadTimeout());
				log.debug("Connected to server {} on port {}", socket.getInetAddress(), socket.getPort());
				listen();
			} catch (IOException e) {
				log.error("Error processing TOTP server messages", e);
				waitForMillis(5000);
			} finally {
				close();
			}
		}
	}

	private void waitForMillis(long sleepTime) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis()-start<sleepTime) {
			try {
				Thread.sleep(sleepTime-(System.currentTimeMillis()-start));
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	private void listen() throws IOException {
		InputStream in = socket.getInputStream();
		while (isRunning()) {
			byte[] lengthHeader = new byte[2];
			int read = in.read(lengthHeader);
			if(read == 2) {
				byte[] message = new byte[getLength(lengthHeader)];
				read = in.read(message);
				if(read == message.length) {
					byte[] ticket = new byte[8];
					byte[] value = new byte[message.length-8];
					System.arraycopy(message, 0, ticket, 0, 8);
					System.arraycopy(message, 8, value, 0, value.length);
					Message request = new Message(ByteBuffer.wrap(ticket).getLong(), value);
					log.debug("Processing message with hashCode: {} for ticket {} with data with hashCode: {}", 
							request.hashCode(),
							request.getTicket(),
							Arrays.hashCode(request.getValue()));
					try {
						totpServerMessageHandler
								.handle(request, this::sendResponse);
					} catch (JWTException | UnconnectedDeviceException e) {
						log.error("Error processing message: {}.{}", e.getMessage(), 
								e.getCause()!=null?" Caused by "+ e.getCause().getMessage():"");
						sendResponse(new Message(request.getTicket(), NORESPONSE));
					}
				}
			}
		}
	}

	private synchronized void sendResponse(Message response) 
			throws IOException {
		log.debug("Sending response for ticket {} with data with hashCode: {}", response.getTicket(),
				Arrays.hashCode(response.getValue()));
		byte[] messageByte = response.toMessageBytes();
		byte[] lengthHeader = getLengthHeader(messageByte);
		OutputStream out = socket.getOutputStream();
		out.write(lengthHeader);
		out.write(messageByte);
		out.flush();
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
			if(socket!=null && !socket.isClosed())
				socket.close();
		} catch (IOException e) {
			log.error("Error closing TOTP client socket", e);
		}
	}
}
