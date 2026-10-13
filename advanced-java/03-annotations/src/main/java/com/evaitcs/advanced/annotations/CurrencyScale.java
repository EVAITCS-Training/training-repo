package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A custom validation sticker: "this BigDecimal must have exactly {@code value} decimal places."
 *
 * SecureBank money is always scale 2 (README, "Shared conventions"), so the default is 2 —
 * a use site can just write {@code @CurrencyScale} and get the house rule for free. This is
 * the DEFAULT-element lesson: give a sensible default and the common case needs no argument.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CurrencyScale {

    /** The required number of decimal places; defaults to 2 because money is scale 2 here. */
    int value() default 2;
}
