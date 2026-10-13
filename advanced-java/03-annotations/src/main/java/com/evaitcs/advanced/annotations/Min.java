package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A custom validation sticker: "this numeric field must be at least {@code value}."
 *
 * RUNTIME retention so MiniValidator can read it with reflection; FIELD target so it only
 * lands on fields. {@code value()} has NO default — it is a REQUIRED element, so every use
 * site must supply it: {@code @Min(0)}. That is how you make an annotation element mandatory.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Min {

    /** The inclusive lower bound. Required — no default means the use site must give it. */
    long value();

    /** Optional override message; empty by default so MiniValidator can build its own. */
    String message() default "";
}
