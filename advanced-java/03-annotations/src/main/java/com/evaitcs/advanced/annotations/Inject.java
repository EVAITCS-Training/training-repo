package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a FIELD the mini-container should fill by type — a 1-line stand-in for Spring's
 * {@code @Autowired}. WireMeDemo's container walks each component's fields, finds the ones
 * carrying {@code @Inject}, and sets them (with reflection, module 02) to the matching bean.
 *
 * No elements: the field's TYPE is enough to choose the dependency, exactly as by-type
 * autowiring works. This annotation plus {@link WireMe} plus ~20 lines is the whole DI idea.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Inject {
}
