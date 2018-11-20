package com.quakearts.auth.server.rest.models;

import javax.validation.constraints.NotNull;

import com.quakearts.auth.server.rest.validators.annotation.ValidSecretKey;

import io.swagger.v3.oas.annotations.media.Schema;

public class Secret {
	@Schema(description="The key for the secret value. Must be in the form {[a-z]+[A-Z].}.", 
			example="{database.password} or {Database.Password} or {database.Password}")
	@ValidSecretKey
	private String key;
	@Schema(description="The secret value")
	@NotNull
	private String value;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Secret withKeyAs(String key) {
		setKey(key);
		return this;
	}
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Secret withValueAs(String value) {
		setValue(value);
		return this;
	}
}
