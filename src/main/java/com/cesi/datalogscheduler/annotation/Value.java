package com.cesi.datalogscheduler.annotation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@SuppressWarnings("unused")
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Value.List.class)
@Documented
@Constraint(
        validatedBy = {
                ValueValidatorForNumber.class,
                ValueValidatorForByte.class,
                ValueValidatorForShort.class,
                ValueValidatorForInteger.class,
                ValueValidatorForLong.class,
                ValueValidatorForCharSequence.class
        }
)
public @interface Value {
    String message() default "必须为指定值";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long[] numbers() default {};

    String[] strings() default {};

    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        Value[] value();
    }
}
