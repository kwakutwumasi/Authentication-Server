package com.quakearts.auth.server.totp.edge.websocket;

import java.io.IOException;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.totp.edge.websocket.model.Payload;

public class JSONConverter implements Decoder.Text<Payload>, Encoder.Text<Payload> {

	private ObjectMapper mapper;
	
	@Override
	public void init(EndpointConfig endpointConfig) {
		mapper = new ObjectMapper();
	}

	@Override
	public void destroy() {
		//Do nothing
	}

	@Override
	public String encode(Payload message) throws EncodeException {
		try {
			return mapper.writeValueAsString(message);
		} catch (JsonProcessingException e) {
			throw new EncodeException(message, "Unable to encode", e);
		}
	}

	@Override
	public Payload decode(String s) throws DecodeException {
		try {
			return mapper.readValue(s, Payload.class);
		} catch (IOException e) {
			throw new DecodeException(s, "Unable to decode", e);
		}
	}

	@Override
	public boolean willDecode(String s) {
		s = s.trim();
		return s.startsWith("{") && s.endsWith("}");
	}

}
