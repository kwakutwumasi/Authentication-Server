package com.quakearts.auth.server.rest.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import com.quakearts.auth.server.rest.validators.annotation.ValidLoginConfigurationEntry;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public class LoginConfiguration implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8418439449230724666L;
	@Schema(description="The name for this collection of authentication modules")
	private String name;
	@ArraySchema(minItems=1,
			schema=@Schema(description="A list of authentication modules to use during authentication",
			implementation=LoginConfigurationEntry.class))
	@Valid
	private List<@ValidLoginConfigurationEntry LoginConfigurationEntry> entries = new ArrayList<>();
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<LoginConfigurationEntry> getEntries() {
		return entries;
	}
	
	public void setEntries(List<LoginConfigurationEntry> entries) {
		this.entries = entries;
	}
}