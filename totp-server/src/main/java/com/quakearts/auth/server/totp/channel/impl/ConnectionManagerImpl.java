package com.quakearts.auth.server.totp.channel.impl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.channel.ConnectionManager;
import com.quakearts.auth.server.totp.exception.InvalidInputException;
import com.quakearts.auth.server.totp.exception.SocketShutdownException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.auth.server.totp.options.TOTPOptions.PerformancePreferences;

@Singleton
public class ConnectionManagerImpl implements ConnectionManager, IncomingBitesProcessingListener {

	private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
	private static final byte[] ECHO = new byte[] {0};
	
	@Inject
	private TOTPOptions totpOptions;
	private ExecutorService executorService;
	private ServerSocket serverSocket;
	private List<DeviceConnection> deviceConnections = new CopyOnWriteArrayList<>();

	private Boolean running = Boolean.FALSE;
	private Object runningLock = new Object();
	private Timer timerService;
	private AtomicLong counter = new AtomicLong();
	private Map<Long, CallbackItem> callbackStore = new ConcurrentHashMap<>();
	
	@Override
	public void init() 
			throws IOException, NoSuchAlgorithmException, KeyStoreException, 
			NoSuchProviderException, CertificateException, UnrecoverableKeyException, 
			KeyManagementException {
		log.debug("Setting up...");
		executorService = Executors.newFixedThreadPool(totpOptions.getDeviceConnectionThreads());
		
		String keystorePassword = totpOptions.getDeviceConnectionKeystorePassword();

		KeyStore keystore = KeyStore.getInstance(totpOptions.getDeviceConnectionKeystoreType(), 
				totpOptions.getDeviceConnectionKeystoreProvider());		
		keystore.load(Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(totpOptions.getDeviceConnectionKeystore()), 
				toCharArrayOrNull(keystorePassword));
		
		char[] keyPassword = toCharArrayOrNull(totpOptions.getDeviceConnectionKeyPassword());
		
		if(keyPassword==null)
			keyPassword = toCharArrayOrNull(keystorePassword);
		
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keystore, keyPassword);
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
		timerService.schedule(new TimerTaskWrapper(this::sendEcho), echoInterval, echoInterval);
		timerService.schedule(new TimerTaskWrapper(this::reapOrphanCallbacks), echoInterval, echoInterval);
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
		log.debug("Setup complete");
	}
	
	private char[] toCharArrayOrNull(String password) {
		return password!=null?password.toCharArray():null;
	}
	
	private void runServer() {
		log.debug("Server socket running...");
		setRunning(true);
		while (isRunning()) {
			Socket socket = accept();
			if(socket!=null) {
				store(socket);
				log.debug("Accepted a connection from {} on port {}", 
						socket.getInetAddress(), socket.getPort());
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
		DeviceConnection connection = new DeviceConnection(socket, this);
		deviceConnections.add(connection);
	}
	
	@Override
	public void shutdown() {
		if(isRunning()) {
			log.debug("Shutting down....");
			setRunning(false);
			try {
				serverSocket.close();
			} catch (IOException e) {
				log.error("Error closing server socket", e);
			}
			timerService.cancel();
			log.debug("Shutdown completely");
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
			send(ECHO, response->{});
			log.debug("Echo sent");
		} catch (TOTPException e) {
			//Do nothing
		}
	}
	
	private void reapOrphanCallbacks() {
		log.debug("Reaper running... {} callbacks pending", callbackStore.size());
		callbackStore.entrySet().forEach(entry->{
			CallbackItem callbackItem = entry.getValue();
			if(System.currentTimeMillis()-callbackItem.timestamp>
				totpOptions.getDeviceConnectionRequestTimeout()+100) {
				callbackStore.remove(entry.getKey());
			}
		});
		log.debug("Reaper complete... {} callbacks remaining", callbackStore.size());
	}
	
	@Override
	public void send(byte[] bites, Consumer<byte[]> callback) throws TOTPException {
		if(bites.length>2039) {
			throw new InvalidInputException();
		}
		
		if(deviceConnections.isEmpty()) {
			return;
		}
		
		long ticket = counter.getAndIncrement();
		log.debug("Sending message with hashCode: {} with ticket {}", Arrays.hashCode(bites), ticket);
		byte[] tosend = new byte[bites.length+8];
		System.arraycopy(ByteBuffer.allocate(8).putLong(ticket).array(), 0, tosend, 0, 8);
		System.arraycopy(bites, 0, tosend, 8, bites.length);
		
		for(DeviceConnection deviceConnection:deviceConnections) {
			 executorService.execute(()->{
				try {
					deviceConnection.send(tosend);
				} catch (SocketShutdownException e) {
					log.debug("Socket {} shutdown.", deviceConnection.getInfo());
					deviceConnections.remove(deviceConnection);
				}
			});
		}
		
		callbackStore.put(ticket, new CallbackItem(callback));
	}

	@Override
	public void processIncoming(byte[] bites) {
		log.debug("Processing response...");
		executorService.execute(()->{
			byte[] ticketBites = new byte[8];
			System.arraycopy(bites, 0, ticketBites, 0, 8);
			byte[] toprocess = new byte[bites.length - 8];
			System.arraycopy(bites, 8, toprocess, 0, bites.length-8);
			long ticket = ByteBuffer.wrap(ticketBites).getLong();
			log.debug("Processing response for ticket {}...", ticket);
			
			CallbackItem callbackItem = callbackStore.remove(ticket);
			if(callbackItem != null) {
				log.debug("Processing call-back for ticket {}...", ticket);
				processCallback(toprocess, callbackItem);
			} else {
				log.info("Ignored message with ticket {}", ticket);
			}
		});
	}

	private void processCallback(byte[] toprocess, CallbackItem callbackItem) {
		callbackItem.callback.accept(toprocess);
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

	class CallbackItem {
		long timestamp = System.currentTimeMillis();
		Consumer<byte[]> callback;
		
		CallbackItem(Consumer<byte[]> callback) {
			this.callback = callback;
		}
	}
}
