package com.quakearts.auth.server.rest.services;

import com.quakearts.auth.server.rest.models.ErrorResponse;

public interface ErrorService {
	ErrorResponse createErrorResponse(String code, Exception...exceptions);
	ErrorResponse createErrorResponse(String code, String explanation);
}
