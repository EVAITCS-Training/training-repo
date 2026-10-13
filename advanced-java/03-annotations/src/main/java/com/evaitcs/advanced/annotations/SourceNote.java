package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SOURCE retention: the compiler records this note, then THROWS IT AWAY. It never reaches the
 * class file, so nothing at runtime — not reflection, not the JVM — can ever see it.
 *
 * This is what {@code @Override} and {@code @SuppressWarnings} use: they are instructions to
 * the COMPILER, and once compilation is done their job is finished. RetentionMatrixDemo proves
 * a field carrying {@code @SourceNote} reports "not present" at runtime.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface SourceNote {

    String value() default "";
}
