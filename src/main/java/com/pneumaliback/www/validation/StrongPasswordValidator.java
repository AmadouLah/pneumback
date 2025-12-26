package com.pneumaliback.www.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private int min;

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "12345678", "password", "motdepasse", "11111111", "00000000",
            "123456789", "1234567", "qwerty", "azerty", "letmein");

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.min = constraintAnnotation.min();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null)
            return false;

        String pwd = value.trim();
        if (pwd.length() < min)
            return false;

        if (pwd.matches("^[0-9]+$"))
            return false;

        boolean hasLower = pwd.matches(".*[a-z].*");
        boolean hasUpper = pwd.matches(".*[A-Z].*");
        boolean hasDigit = pwd.matches(".*[0-9].*");
        boolean hasSpecial = pwd.matches(".*[^A-Za-z0-9].*");
        int buckets = (hasLower ? 1 : 0) + (hasUpper ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        if (buckets < 3)
            return false;

        if (isSequential(pwd) || isRepeated(pwd))
            return false;

        if (COMMON_PASSWORDS.contains(pwd.toLowerCase()))
            return false;

        return true;
    }

    private boolean isSequential(String s) {
        if (s.length() < 6)
            return false;
        int incRun = 1;
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) - s.charAt(i - 1) == 1) {
                incRun++;
                if (incRun >= 6)
                    return true;
            } else {
                incRun = 1;
            }
        }
        return false;
    }

    private boolean isRepeated(String s) {
        int[] counts = new int[256];
        for (char c : s.toCharArray()) {
            if (c < 256)
                counts[c]++;
        }
        int max = 0;
        for (int c : counts)
            max = Math.max(max, c);
        return max >= Math.ceil(s.length() * 0.7);
    }
}
