package com.quakearts.auth.server.rest.validators;

import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import com.quakearts.auth.server.rest.validators.annotation.ValidRegistrationOption;
import com.quakearts.webapp.security.auth.JWTLoginModule;
import com.quakearts.webapp.security.jwt.impl.HSSigner;
import com.quakearts.webapp.security.jwt.impl.KeyStoreSignerBase;

public class RegistrationOptionValidator implements ConstraintValidator<ValidRegistrationOption, String> {

	private static final Set<String> validOptions = new HashSet<>();
	
	static {
		validOptions.add(JWTLoginModule.ACTIVATEAFTERPARAMETER);
		validOptions.add(JWTLoginModule.ACTIVATEAFTERPERIODPARAMETER);
		validOptions.add(JWTLoginModule.ADDITIONALCLAIMSPARAMETER);
		validOptions.add(JWTLoginModule.GRACEPERIODPARAMETER);
		validOptions.add(JWTLoginModule.VALIDITY_PERIODPARAMETER);
		validOptions.add(JWTLoginModule.VALIDITYPARAMETER);
		validOptions.add(JWTLoginModule.ISSUERPARAMETER);
		validOptions.add(JWTLoginModule.AUDIENCEPARAMETER);
		validOptions.add(JWTLoginModule.ALGORITHMPARAMETER);
		validOptions.add(HSSigner.SECRETPARAMETER);
		validOptions.add(HSSigner.SECRETPARAMETERHEX);
		validOptions.add(KeyStoreSignerBase.ALIASPARAMETER);
		validOptions.add(KeyStoreSignerBase.FILEPARAMETER);
		validOptions.add(KeyStoreSignerBase.STORECREDENTIALSPARAMETER);
		validOptions.add(KeyStoreSignerBase.STORE_TYPEPARAMETER);
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		boolean returnValue = true;
		HibernateConstraintValidatorContext hibernateContext = 
				context.unwrap(HibernateConstraintValidatorContext.class);

		if(!validOptions.contains(value)){
			hibernateContext.addExpressionVariable("key", value)
				.buildConstraintViolationWithTemplate("{option.key.invalid}")
				.addConstraintViolation();
			returnValue = false;
		}
		return returnValue;
	}

}
