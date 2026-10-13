package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A custom validation sticker: "this field must not be null."
 *
 * WHY RUNTIME retention: MiniValidator is a READER that runs while the program runs, and it
 * finds this note with reflection (module 02). Only RUNTIME-retained annotations are visible
 * to reflection — SOURCE would be discarded by the compiler and CLASS would sit unread in the
 * class file. This is the whole lesson of RetentionMatrixDemo, applied.
 *
 * WHY FIELD target: it may only be stuck on fields, so a misplaced use is a compile error.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotNull {

    /** The message a real framework would surface; defaulted so it is optional at the use site. */
    String message() default "must not be null";
}
