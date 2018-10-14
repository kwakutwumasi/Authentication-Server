package com.quakearts.auth.server.rest.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.quakearts.auth.server.rest.models.Registration;
import com.quakearts.auth.server.rest.validators.annotation.ValidUpdatedRegistration;

public class RegistrationValidator extends RegistrationValidatorBase
	implements ConstraintValidator<ValidUpdatedRegistration, Registration> {
	
	@Override
	public boolean isValid(Registration value, ConstraintValidatorContext context) {
		return checkValue(value, context) && super.isValid(value, context);
	}
	
}
