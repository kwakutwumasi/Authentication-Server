package com.quakearts.auth.server.totp.channel.impl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.channel.ConnectionManager;
import com.quakearts.auth.server.totp.channel.DeviceConnection;
import com.quakearts.auth.server.totp.exception.InvalidInputException;
import com.quakearts.auth.server.totp.exception.SocketShutdownException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.auth.server.totp.options.TOTPOptions.PerformancePreferences;

@Singleton
public class ConnectionManagerImpl implements ConnectionManager {

	private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

	private static final byte[] NORESPONSE = new byte[]{(byte) 255};

	private static final byte[] ECHO = new byte[]{0};
	
	@Inject
	private TOTPOptions totpOptions;
	private ExecutorService executorService;
	private ServerSocket serverSocket;
	private List<DeviceConnection> deviceConnections = new ArrayList<>();

	private Boolean running = Boolean.FALSE;
	private Object runningLock = new Object();

	private List<DeviceConnection> deadConnections = new ArrayList<>();

	private Timer timerService;
	
	@Override
	public void init() 
			throws IOException, NoSuchAlgorithmException, KeyStoreException, 
			NoSuchProviderException, CertificateException, UnrecoverableKeyException, 
			KeyManagementException {
		executorService = Executors.newFixedThreadPool(totpOptions.getDeviceConnectionThreads());
		
		String keystorePassword = totpOptions.getDeviceConnectionKeystorePassword();

		KeyStore keystore = KeyStore.getInstance(totpOptions.getDeviceConnectionKeystoreType(), 
				totpOptions.getDeviceConnectionKeystoreProvider());		
		keystore.load(Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(totpOptions.getDeviceConnectionKeystore()), 
				toCharArrayOrNull(keystorePassword));
		
		String keyPassword = totpOptions.getDeviceConnectionKeyPassword();
		
		if(keyPassword==null)
			keyPassword = keystorePassword;
		
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keystore, toCharArrayOrNull(keyPassword));
		SSLContext context = SSLContext.getInstance(totpOptions.getDeviceConnectionSSLInstance());
		
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(keystore);
		
		context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		
		serverSocket = context.getServerSocketFactory().createServerSocket(totpOptions.getDeviceConnectionPort());
		int deviceConnectionReceiveBufferSize = totpOptions.getDeviceConnectionReceiveBufferSize();
		if(deviceConnectionReceiveBufferSize>0) {
			serverSocket.setReceiveBufferSize(deviceConnectionReceiveBufferSize);
		}
		
		PerformancePreferences preferences = totpOptions.getDeviceConnectionPerformancePreferences();
		if(preferences!=null) {
			serverSocket.setPerformancePreferences(preferences.getConnectionTime(),
					preferences.getLatency(), preferences.getBandwidth());
		}
		
		Boolean deviceConnectionReuseAddress = totpOptions.getDeviceConnectionReuseAddress();
		if(deviceConnectionReuseAddress != null) {
			serverSocket.setReuseAddress(deviceConnectionReuseAddress);
		}
		
		executorService.execute(this::runServer);
		timerService = new Timer();
		long echoInterval = totpOptions.getDeviceConnectionEchoInterval();
		timerService.schedule(new TimerTaskWrapper(this::sendEcho), echoInterval, 
					echoInterval);
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
	}
	
	private char[] toCharArrayOrNull(String password) {
		return password!=null?password.toCharArray():null;
	}
	
	private void runServer() {
		setRunning(true);
		while (isRunning()) {
			Socket socket = accept();
			if(socket!=null) {
				store(socket);
			}
		}
	}
	
	private Socket accept() {
		Socket socket = null;
		try {
			socket = serverSocket.accept();
			socket.setSoTimeout(totpOptions.getDeviceConnectionSocketTimeout());
			socket.setKeepAlive(true);
		} catch (SocketException e) {
			if(!isRunning())
				log.info("Device Connection Shutdown");
			
		} catch (IOException e) {
			log.error("Error running socket accept", e);
		}
		return socket;
	}

	private void store(Socket socket) {
		DeviceConnection connection = new DeviceConnection(socket);
		try {
			byte[] response = connection.send(ECHO);
			if(response.length == 1 && (response[0] & 0xff) == 0) {
				synchronized(deviceConnections) {
					deviceConnections.add(connection);
				}
			}
		} catch (SocketShutdownException | InvalidInputException e) {
			log.error("Unable to process socket connection", e);
		}
	}
	
	@Override
	public void shutdown() {
		if(isRunning()) {
			setRunning(false);
			try {
				serverSocket.close();
			} catch (IOException e) {
				log.error("Error closing server socket", e);
			}
			timerService.cancel();
		}
	}
	
	class TimerTaskWrapper extends TimerTask {
		
		Runnable task;
		
		public TimerTaskWrapper(Runnable task) {
			this.task = task;
		}

		@Override
		public void run() {
			task.run();
		}
	}
	
	private void sendEcho() {
		try {
			send(ECHO);
		} catch (UnconnectedDeviceException e) {
			//Do nothing
		}		
	}
	
	@Override
	public byte[] send(byte[] bites) throws UnconnectedDeviceException {
		synchronized(deviceConnections) {
			List<Future<byte[]>> responses = process(bites);
			try {
				for(Future<byte[]> response:responses) {
					try {
						byte[] responseBites = response.get();
						if(responseBites.length > 1) {
							return responseBites;
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} catch (ExecutionException e) {
						//Ignore
					}
				}
			} finally {
				for(DeviceConnection deviceConnection:deadConnections) {
					deviceConnections.remove(deviceConnection);
				}
				
				deadConnections.clear();					
			}
			
			if(bites.length==1 && (bites[0] & 0xff)==0)
				return ECHO;
			
			throw new UnconnectedDeviceException("Connections returned no info");
		}
	}

	private List<Future<byte[]>> process(byte[] bites) {
		List<Future<byte[]>> responses = new ArrayList<>();
		for(DeviceConnection deviceConnection:deviceConnections) {
			responses.add(CompletableFuture.supplyAsync(()->{
				try {
					return deviceConnection.send(bites);
				} catch (SocketShutdownException e) {
					synchronized (deadConnections) {
						deadConnections.add(deviceConnection);
					}
					return NORESPONSE;
				} catch (InvalidInputException e) {
					return NORESPONSE;
				}
			}, executorService));
		}
		return responses;
	}

	private boolean isRunning() {
		synchronized (runningLock) {
			return running;
		}
	}

	private void setRunning(boolean running) {
		synchronized (runningLock) {
			this.running = running;
		}
	}

}
