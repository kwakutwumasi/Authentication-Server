package com.quakearts.auth.server.rest.validators.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.quakearts.auth.server.rest.validators.NewRegistrationValidator;

@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD, PARAMETER })
@Constraint(validatedBy=NewRegistrationValidator.class)
public @interface ValidNewRegistration {
	String message() default "{invalid.registration}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default{};
}
