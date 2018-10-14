package com.quakearts.auth.server.rest.validators;

import javax.validation.ConstraintValidatorContext;

import com.quakearts.auth.server.rest.models.Registration;
import com.quakearts.webapp.security.auth.JWTLoginModule;
import com.quakearts.webapp.security.jwt.signature.ESSignature.ESAlgorithmType;
import com.quakearts.webapp.security.jwt.signature.HMac.HSAlgorithmType;
import com.quakearts.webapp.security.jwt.signature.RSASignature.RSAAlgorithmType;

public class RegistrationValidatorBase {
	public boolean isValid(Registration value, ConstraintValidatorContext context) {
		return checkAlias(value, context)
				&& checkConfigurationsEmpty(value, context) 
				&& checkPeriod(value, context)
				&& checkAlgorithm(value, context);
	}

	protected boolean checkValue(Registration value, ConstraintValidatorContext context) {
		if(value == null) {
			context.buildConstraintViolationWithTemplate("{null.registration}")
			.addConstraintViolation();
			return false;
		}
		return true;
	}

	private boolean checkAlias(Registration value, ConstraintValidatorContext context) {
		if(value.getAlias()==null 
				|| value.getAlias().trim().isEmpty()) {
			context.buildConstraintViolationWithTemplate("{null.registration.alias}").
		    addConstraintViolation();
			return false;
		}
		return true;
	}

	private boolean checkConfigurationsEmpty(Registration value, ConstraintValidatorContext context) {
		if(value.getConfigurations().isEmpty()) {
			context.buildConstraintViolationWithTemplate("{login.configuration.required}")
			.addConstraintViolation();
			return false;
		}
		return true;
	}

	private boolean checkPeriod(Registration value, ConstraintValidatorContext context) {
		if(value.getOptions().containsKey(JWTLoginModule.VALIDITY_PERIODPARAMETER)
				&& checkPeriodValue(value.getOptions()
				.get(JWTLoginModule.VALIDITY_PERIODPARAMETER), context)) {
			context.buildConstraintViolationWithTemplate("{validity.period.invalid}")
			.addConstraintViolation();
			return false;
		}
		if(value.getOptions().containsKey(JWTLoginModule.ACTIVATEAFTERPERIODPARAMETER)
				&& checkPeriodValue(value.getOptions()
				.get(JWTLoginModule.ACTIVATEAFTERPERIODPARAMETER), context)) {
			context.buildConstraintViolationWithTemplate("{activate.after.period.invalid}")
			.addConstraintViolation();
			return false;
		}
		return true;
	}
	
	private boolean checkPeriodValue(String value, ConstraintValidatorContext context) {
		String[] expiresInStringParts = value.split("[\\s]+", 2);
		return expiresInStringParts.length != 2 
				|| expiresInStringParts[0].trim().isEmpty()
				|| expiresInStringParts[1].trim().isEmpty();
	}

	private boolean checkAlgorithm(Registration value, ConstraintValidatorContext context) {
		String algorithm = value.getOptions().get("algorithm");
		if(algorithm !=null && (!checkHS(algorithm)
				&& !checkES(algorithm)
				&& !checkRS(algorithm))) {
			context.buildConstraintViolationWithTemplate("{algorithm.invalid}")
				.addConstraintViolation();
			return false;
		}
		
		return true;
	}

	private boolean checkHS(String algorithm) {
		try {
			HSAlgorithmType.valueOf(algorithm);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	private boolean checkES(String algorithm) {
		try {
			RSAAlgorithmType.valueOf(algorithm);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	private boolean checkRS(String algorithm) {
		try {
			ESAlgorithmType.valueOf(algorithm);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
