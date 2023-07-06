package com.cesi.datalogscheduler.annotation;

@SuppressWarnings("unused")
public class ValueValidatorForNumber extends AbstractValueValidator<Number> {
    public ValueValidatorForNumber() {
    }

    @Override
    protected boolean contains(Number value) {
        for (long number : numbers) {
            if (value.longValue() == number) {
                return true;
            }
        }
        return false;
    }
}
