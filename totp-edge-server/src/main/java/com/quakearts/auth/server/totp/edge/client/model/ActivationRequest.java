package com.quakearts.auth.server.totp.edge.client.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ActivationRequest {
	private String token;
	private String alias;
	@JsonIgnore
	private Map<String, Object> otherAttributes = new HashMap<>(); 

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	@JsonAnySetter
	public void addAttribute(String key, Object value) {
		otherAttributes.put(key, value);
	}
	
	@JsonIgnore
	@JsonAnyGetter
	public Map<String, Object> getOtherAttributes() {
		return otherAttributes;
	}
}
