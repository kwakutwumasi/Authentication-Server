package com.quakearts.auth.server.totp.channel.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.quakearts.auth.server.totp.exception.SocketShutdownException;

public class DeviceConnection {

	private Socket socket;
	private IncomingBitesProcessingListener listener;
	private boolean running = false;
	
	public DeviceConnection(Socket socket, IncomingBitesProcessingListener listener) {
		this.socket = socket;
		this.listener = listener;
		new Thread(this::receive).start();
	}
	
	private void receive() {
		running = true;
		InputStream in;
		try {
			in = socket.getInputStream();
			while (running) {
				byte[] lengthHeader = new byte[2];
				int read = in.read(lengthHeader);
				if(read == lengthHeader.length) {
					byte[] response = new byte[getLength(lengthHeader)];
					read = in.read(response);
					if(read == response.length)
						listener.processIncoming(response);
				}
			}
		} catch (IOException e) {
			running = false;
		}
	}
	
	public synchronized void send(byte[] bites)
			throws SocketShutdownException {
		try {
			OutputStream out = socket.getOutputStream();
			byte[] lengthHeader = getLengthHeader(bites);
			out.write(lengthHeader, 0, lengthHeader.length);
			out.write(bites, 0, bites.length);
			out.flush();			
		} catch (IOException e) {
			try {
				socket.close();
			} catch (IOException e1) {
				//Do nothing
			}
			
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

}
