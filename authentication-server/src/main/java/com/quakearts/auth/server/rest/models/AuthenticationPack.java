package com.quakearts.auth.server.rest.models;
import java.time.Duration;
import java.util.Map;

import javax.security.auth.login.Configuration;

public class AuthenticationPack {
	Configuration configuration;
	Map<String, String> moduleOptions;
	long expiresIn;
	
	public AuthenticationPack(Registration registration) {
		configuration = new com.quakearts.auth.server.rest.models.AuthConfiguration(registration);
		moduleOptions = registration.getOptions();
		String expiresInString = registration.getOptions().get("validity.period");
		String[] expiresInStringParts = expiresInString.split("[\\s]+", 2);		
		int periodAmount = Integer.parseInt(expiresInStringParts[0].trim());
		String prefix = expiresInStringParts[1].trim().substring(0, 1).toUpperCase();
		Duration duration = Duration.parse("P" + (prefix.equals("H") 
				|| prefix.equals("M") 
				|| prefix.equals("S") ? "T" : "")
						+ periodAmount + prefix);

		expiresIn = duration.getSeconds();
	}
	
	public Configuration getConfiguration() {
		return configuration;
	}
	
	public Map<String, String> getModuleOptions() {
		return moduleOptions;
	}
	
	public long getExpiresIn() {
		return expiresIn;
	}
}