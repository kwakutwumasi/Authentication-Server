package com.quakearts.auth.server.totp.utils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import com.quakearts.webapp.security.util.HashPassword;

public class MaskUtil {
	private MaskUtil() {}
	
	public static String mask(Map<String, String> message){
		return message.entrySet().stream().map(entry->entry.getKey()+"=" +mask(entry.getValue()))
				.collect(Collectors.joining(","));
	}
	
	private static String mask(String value){
		return new HashPassword(value, "SHA-1", 0, "")
				.toString().toUpperCase();
	}
	
	public static String mask(byte[] bites){
		return new HashPassword(new String(bites, StandardCharsets.UTF_8),"SHA-1",0,"").toString()
				.toUpperCase();
	}
}
