package com.quakearts.auth.server.rest.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.quakearts.auth.server.rest.models.Registration;
import com.quakearts.auth.server.rest.validators.annotation.ValidNewRegistration;

public class NewRegistrationValidator extends RegistrationValidatorBase
	implements ConstraintValidator<ValidNewRegistration, Registration> {

	@Override
	public boolean isValid(Registration value, ConstraintValidatorContext context) {
		if(checkValue(value, context) 
				&& checkId(value, context)) {
			return super.isValid(value, context);
		}
		
		return false;
	}

	private boolean checkId(Registration value, ConstraintValidatorContext context) {
		if(value.getId()==null  || value.getId().trim().isEmpty()){
			context.buildConstraintViolationWithTemplate("{null.registration.id}").
		    addConstraintViolation();
			return false;
		}
		return true;
	}

}
