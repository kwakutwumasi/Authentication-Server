package com.quakearts.auth.server.rest.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.quakearts.auth.server.rest.validators.annotation.ValidDataSourceKey;
public class DataSourceKeyValidator 
	implements ConstraintValidator<ValidDataSourceKey, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value != null && value.matches("[a-zA-Z0-9]+$");
	}
}
