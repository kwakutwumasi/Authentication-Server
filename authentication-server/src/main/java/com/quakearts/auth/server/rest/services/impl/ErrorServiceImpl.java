package com.quakearts.auth.server.rest.services.impl;

import javax.inject.Singleton;

import com.quakearts.auth.server.rest.models.ErrorResponse;
import com.quakearts.auth.server.rest.services.ErrorService;

@Singleton
public class ErrorServiceImpl implements ErrorService {

	@Override
	public ErrorResponse createErrorResponse(String code, Exception... exceptions) {
		ErrorResponse errorResponse = new ErrorResponse().setCodeAs(code);
		for(Exception exception:exceptions) {
			errorResponse.addExplanation(exception.getMessage());			
			if(exception.getCause()!=null) {
				errorResponse.addExplanation("Caused by: "+exception.getCause().getMessage());
			}
		}
		return errorResponse;
	}

	@Override
	public ErrorResponse createErrorResponse(String code, String explanation) {
		return new ErrorResponse().setCodeAs(code).addExplanation(explanation);			
	}

}
