package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A METHOD annotation WITHOUT {@code @Inherited}, used to prove the exact nuance:
 * method annotations are NEVER inherited by an overriding method.
 *
 * Even if we added {@code @Inherited} here it would make no difference — {@code @Inherited}
 * only affects class-level annotations. So when {@code ChildService} overrides a
 * {@code @Logged} method from {@code BaseService}, the override carries NO {@code @Logged}.
 * RepeatableInheritedDemo asserts exactly this: the base method has it, the override does not.
 *
 * This is why frameworks that read method annotations must walk the type hierarchy themselves
 * (Spring's {@code AnnotatedElementUtils} does precisely that) — the JDK will not hand it over.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Logged {

    String value() default "logged";
}
