package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CLASS retention: the note IS written into the class file, but the JVM does NOT load it into
 * memory at runtime. So bytecode tools (build plugins, instrumenters, ProGuard) can read it,
 * while ordinary reflection cannot. CLASS is the DEFAULT retention when you omit {@code @Retention}.
 *
 * The line between CLASS and RUNTIME is "does reflection see it?" — CLASS says no. That is why
 * a framework annotation that must be read at runtime MUST declare RUNTIME explicitly.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface ClassNote {

    String value() default "";
}
