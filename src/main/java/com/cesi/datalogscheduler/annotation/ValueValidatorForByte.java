package com.cesi.datalogscheduler.annotation;

public class ValueValidatorForByte extends AbstractValueValidator<Byte> {
    public ValueValidatorForByte() {
    }

    @Override
    protected boolean contains(Byte value) {
        for (long number : numbers) {
            if (value.longValue() == number) {
                return true;
            }
        }
        return false;
    }
}
