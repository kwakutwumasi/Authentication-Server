package com.quakearts.auth.server.rest.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.quakearts.auth.server.rest.validators.annotation.ValidSecretKey;
public class SecretKeyValidator 
	implements ConstraintValidator<ValidSecretKey, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value != null && value.matches("\\{[a-zA-Z\\.\\s]+\\}$");
	}
}
