package com.quakearts.auth.server.rest.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.quakearts.auth.server.rest.models.LoginConfiguration;
import com.quakearts.auth.server.rest.validators.annotation.ValidLoginConfiguration;

public class LoginConfigurationValidator 
	implements ConstraintValidator<ValidLoginConfiguration, LoginConfiguration> {

	@Override
	public boolean isValid(LoginConfiguration value, ConstraintValidatorContext context) {
		boolean returnValue = true;
		if(value.getName() == null 
				|| value.getName().trim().isEmpty()){
			context.buildConstraintViolationWithTemplate("{name.required}")
			.addConstraintViolation();
			returnValue = false;
		} else if(value.getEntries().isEmpty()) {
			context.buildConstraintViolationWithTemplate("{login.configuration.entry.required}")
			.addConstraintViolation();
			returnValue = false;
		}
		
		return returnValue;
	}

}
