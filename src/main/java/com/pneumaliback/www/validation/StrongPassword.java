package com.pneumaliback.www.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface StrongPassword {

    String message() default "Mot de passe trop faible: minimum {min} caractères, mélangez lettres, chiffres et caractères spéciaux, évitez les suites et mots de passe courants.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int min() default 8;
}
