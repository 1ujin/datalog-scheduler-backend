package com.cesi.datalogscheduler.annotation;

@SuppressWarnings("unused")
public class ValueValidatorForCharSequence extends AbstractValueValidator<CharSequence> {
    public ValueValidatorForCharSequence() {
    }

    @Override
    protected boolean contains(CharSequence value) {
        for (String string : strings) {
            if (string == null) {
                continue;
            }
            if (string.contentEquals(value)) {
                return true;
            }
        }
        return false;
    }
}
