package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RUNTIME retention: the note is written to the class file AND loaded into memory, so
 * reflection can read it. This is the ONLY retention a runtime reader (like MiniValidator,
 * or Spring) can see — which is why every framework annotation you will meet declares it.
 *
 * It also demonstrates ELEMENTS and DEFAULTS:
 *   - {@code value()} has no default, so it is REQUIRED at the use site.
 *   - {@code level()} defaults to 1, so a use site may omit it and still get a value.
 * RetentionMatrixDemo proves both: {@code @RuntimeNote("x")} yields level()==1.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RuntimeNote {

    /** Required element — every use site must supply it. */
    String value();

    /** Optional element with a default — omit it and you still get 1. */
    int level() default 1;
}
