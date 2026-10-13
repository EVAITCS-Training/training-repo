package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A REPEATABLE annotation: you may stick it on the same element more than once.
 *
 * Java has no native "list of annotations," so {@code @Repeatable} names a CONTAINER type
 * ({@link Audits}) that holds an array. When you write {@code @Audit("a") @Audit("b")} on one
 * class, the compiler quietly packs both into a single {@code @Audits({@Audit("a"), @Audit("b")})}.
 *
 * The consequence, proven in RepeatableInheritedDemo: {@code getAnnotation(Audit.class)} returns
 * null (the single form is gone, folded into the container), while
 * {@code getAnnotationsByType(Audit.class)} unpacks the container and returns BOTH. Always use
 * getAnnotationsByType for a repeatable annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Audits.class)
public @interface Audit {

    /** What is being audited, e.g. "login" or "transfer". */
    String value();
}
