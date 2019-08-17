package com.quakearts.auth.server.totp.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.quakearts.auth.server.totp.exception.InvalidInputException;
import com.quakearts.auth.server.totp.exception.SocketShutdownException;

public class DeviceConnection {

	private Socket socket;
	
	public DeviceConnection(Socket socket) {
		this.socket = socket;
	}
	
	public byte[] send(byte[] bites) throws SocketShutdownException, InvalidInputException {
		if(bites.length>2047) {
			throw new InvalidInputException();
		}
		try {
			OutputStream out = socket.getOutputStream();
			byte[] lengthHeader = getLengthHeader(bites);
			out.write(lengthHeader, 0, lengthHeader.length);
			out.write(bites, 0, bites.length);
			out.flush();
			InputStream in = socket.getInputStream();
			int read = in.read(lengthHeader);
			assert(read == lengthHeader.length);
			byte[] response = new byte[getLength(lengthHeader)];
			read = in.read(response);
			assert(read == response.length);
			
			return response;
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
