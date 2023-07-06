package com.cesi.datalogscheduler.annotation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public abstract class AbstractValueValidator<T> implements ConstraintValidator<Value, T> {
    protected long[] numbers;

    protected String[] strings;

    public AbstractValueValidator() {
    }

    public void initialize(Value value) {
        strings = value.strings();
        numbers = value.numbers();
    }

    public boolean isValid(T value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true;
        } else {
            return contains(value);
        }
    }

    protected abstract boolean contains(T value);
}
