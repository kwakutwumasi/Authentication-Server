package com.quakearts.auth.server.rest.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.quakearts.auth.server.rest.models.LoginConfigurationEntry;
import com.quakearts.auth.server.rest.validators.annotation.ValidLoginConfigurationEntry;

public class LoginConfigurationEntryValidator 
	implements ConstraintValidator<ValidLoginConfigurationEntry, LoginConfigurationEntry> {

	@Override
	public boolean isValid(LoginConfigurationEntry value, ConstraintValidatorContext context) {
		boolean returnValue = true;
		if(value.getModuleClassname()==null 
				|| value.getModuleClassname().trim().isEmpty()){
			context.buildConstraintViolationWithTemplate("{moduleClassname.required}")
			.addConstraintViolation();
			returnValue = false;
		} else if(value.getModuleFlag() == null){
			context.buildConstraintViolationWithTemplate("{moduleFlag.required}")
			.addConstraintViolation();
			returnValue = false;
		}
		return returnValue;
	}

}
