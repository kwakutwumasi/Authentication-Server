package com.quakearts.auth.tests.authentication.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.rest.models.ErrorResponse;
import com.quakearts.auth.server.rest.services.ErrorService;

@RunWith(MainRunner.class)
public class ErrorServiceImplTest {

	@Inject
	private ErrorService errorService;
	
	@Test
	public void testCreateErrorResponseStringExceptionArray() {
		ErrorResponse errorResponse = errorService.createErrorResponse("test-code", 
				new IOException("Test1", new NullPointerException("Test1 cause")),
				new IllegalArgumentException("Test2"));
		assertThat(errorResponse.getCode(), is("test-code"));
		assertThat(errorResponse.getExplanations().size(), is(3));
		assertThat(errorResponse.getExplanations().get(0), is("Test1"));
		assertThat(errorResponse.getExplanations().get(1), is("Caused by: Test1 cause"));
		assertThat(errorResponse.getExplanations().get(2), is("Test2"));	
	}

	@Test
	public void testCreateErrorResponseStringString() {
		ErrorResponse errorResponse = errorService.createErrorResponse("test-code", "Test1");
		assertThat(errorResponse.getCode(), is("test-code"));
		assertThat(errorResponse.getExplanations().size(), is(1));
		assertThat(errorResponse.getExplanations().get(0), is("Test1"));
	}

}
