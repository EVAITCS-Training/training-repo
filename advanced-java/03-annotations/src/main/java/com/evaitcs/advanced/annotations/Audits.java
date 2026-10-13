package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The CONTAINER annotation required by {@link Audit}'s {@code @Repeatable}.
 *
 * The rules are strict and worth memorizing: the container must
 *   1. have a single element named {@code value} whose type is an ARRAY of the repeatable annotation,
 *   2. have a retention at least as long as the repeatable annotation (both RUNTIME here), and
 *   3. be applicable to at least the same targets (TYPE here).
 * You never write {@code @Audits} yourself — the compiler generates it when it sees two {@code @Audit}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Audits {

    /** The packed array of {@link Audit} notes the compiler collected. */
    Audit[] value();
}
