package com.quakearts.auth.server.rest.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public class ErrorResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3997647070964093985L;
	@Schema(description="The application error code. The codes are as follows: "
			+ "invalid-data - there was a problem with the submitted data;"
			+ "invalid-id - an object with the ID cannot be found;"
			+ "invalid-credentials - the client credentials were invalid;"
			+ "existing-id - the ID/alias supplied is not unique;")
	private String code;
	@ArraySchema(schema=@Schema(description="The application error code's explanations"))
	private List<String> explanations = new ArrayList<>();

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@JsonIgnore
	public ErrorResponse withCodeAs(String code) {
		setCode(code);
		return this;
	}
	
	public List<String> getExplanations() {
		return explanations;
	}

	public void setExplanation(List<String> explanations) {
		this.explanations = explanations;
	}
	
	public ErrorResponse addExplanation(String explanation) {
		explanations.add(explanation);
		return this;
	}
}
