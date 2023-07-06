package com.cesi.datalogscheduler.annotation;

@SuppressWarnings("unused")
public class ValueValidatorForLong extends AbstractValueValidator<Long> {
    public ValueValidatorForLong() {
    }

    @Override
    protected boolean contains(Long value) {
        for (long number : numbers) {
            if (value == number) {
                return true;
            }
        }
        return false;
    }
}
