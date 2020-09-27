package com.quakearts.auth.server.totp.channel.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.exception.SocketShutdownException;

public class DeviceConnection {

	private static final Logger log = LoggerFactory.getLogger(DeviceConnection.class);
	private Socket socket;
	private IncomingBitesProcessingListener listener;
	private boolean running = false;
	
	public DeviceConnection(Socket socket, IncomingBitesProcessingListener listener) {
		this.socket = socket;
		this.listener = listener;
		new Thread(this::receive,"IO-"+socket.getInetAddress()+":"+socket.getPort()).start();
	}
	
	private void receive() {
		log.debug("Receive thread started. Processing incoming messages from {} on port {}", 
				socket.getInetAddress(), socket.getPort());
		running = true;
		InputStream in;
		try {
			in = socket.getInputStream();
		} catch (IOException e) {
			log.debug("Unable to open input stream for {} ", getInfo());
			log.debug("", e);
			return;
		}
		while (running) {
			try {
				readAndProcess(in);
			} catch (IOException e) {
				log.debug("Unable to read from input stream {} ", getInfo());
				log.debug("", e);
				running = false;
				try {
					if(!socket.isClosed()) {
						socket.close();
					}
				} catch (IOException e1) {
					log.debug("Socket close error. ", e1);
				}
			}
		}

		log.debug("Receive thread ended {}", getInfo());
	}

	private void readAndProcess(InputStream in) throws IOException {
		byte[] lengthHeader = new byte[2];
		int read = in.read(lengthHeader);
		if(read == lengthHeader.length) {
			byte[] response = new byte[getLength(lengthHeader)];
			read = in.read(response);
			if(read == response.length) {
				listener.processIncoming(response);
			} else {
				log.debug("Received invalid message. Expected length {}. Received length {}", response.length, read);
			}
		} else if(read == -1) {
			throw new IOException("Reached end of stream. InputStream has been closed");
		} else {
			log.debug("Received invalid header. Expected length 2. Received length {}", read);
		}
	}
	
	public synchronized void send(byte[] bites)
			throws SocketShutdownException {
		log.debug("Sending data with hashCode: {}", Arrays.hashCode(bites));
		try {
			OutputStream out = socket.getOutputStream();
			byte[] lengthHeader = getLengthHeader(bites);
			out.write(lengthHeader, 0, lengthHeader.length);
			out.write(bites, 0, bites.length);
			out.flush();
			log.debug("Sent data with hashCode: {}", Arrays.hashCode(bites));
		} catch (Exception e) {
			running = false;
			try {
				socket.close();
			} catch (IOException e1) {
				log.debug("Socket close error. ", e);
			}
			log.debug("Send stream ended {}", getInfo());			
			throw new SocketShutdownException(e);
		}
	}

	private byte[] getLengthHeader(byte[] bites) {
		byte[] lengthHeader = new byte[2];
		lengthHeader[0] = (byte) (bites.length / 8);
		lengthHeader[1] = (byte) (bites.length % 8);
		
		return lengthHeader;
	}

	private int getLength(byte[] lengthHeader) {
		return (lengthHeader[0]*8 + lengthHeader[1])&0x07ff;
	}

	public Object getInfo() {
		return socket!=null?"from "+socket.getRemoteSocketAddress()+" on port "+socket.getPort():"(not connected)";
	}

}
