package com.cesi.datalogscheduler.annotation;

public class ValueValidatorForShort extends AbstractValueValidator<Short> {
    public ValueValidatorForShort() {
    }

    @Override
    protected boolean contains(Short value) {
        for (long number : numbers) {
            if (value.longValue() == number) {
                return true;
            }
        }
        return false;
    }
}
