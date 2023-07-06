package com.cesi.datalogscheduler.annotation;

@SuppressWarnings("unused")
public class ValueValidatorForInteger extends AbstractValueValidator<Integer> {
    public ValueValidatorForInteger() {
    }

    @Override
    protected boolean contains(Integer value) {
        for (long number : numbers) {
            if (value.longValue() == number) {
                return true;
            }
        }
        return false;
    }
}
