package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a COMPONENT the mini-container should instantiate — a 1-line stand-in for
 * Spring's {@code @Component}/{@code @Service}. WireMeDemo reads it with reflection, builds one
 * instance per marked class, and registers it under a name.
 *
 * The {@code value()} element is the bean name; when omitted (empty default) the container
 * derives one from the class's simple name. That mirrors how Spring names beans by default.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WireMe {

    /** The bean name; empty means "derive it from the simple class name." */
    String value() default "";
}
