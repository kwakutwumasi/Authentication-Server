package com.quakearts.auth.tests.authentication.test.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ErrorResponse implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3997647070964093985L;
	private String code;
	private List<String> explanations = new ArrayList<>();

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public ErrorResponse setCodeAs(String code) {
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
