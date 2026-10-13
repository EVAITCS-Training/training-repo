package com.evaitcs.advanced.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An {@code @Inherited} annotation, stuck on a CLASS.
 *
 * {@code @Inherited} means: if a class carries this annotation, its SUBCLASSES report it too
 * via {@code getAnnotation}. RepeatableInheritedDemo proves a {@code ChildService extends
 * BaseService} answers "yes" to {@code getAnnotation(SecureService.class)} even though the
 * sticker is only written on {@code BaseService}.
 *
 * The nuance students always miss: {@code @Inherited} works ONLY for
 *   - class-to-subclass (extends), and
 *   - ONLY through {@code getAnnotation}/{@code getAnnotationsByType} on the Class.
 * It does NOT cross implemented interfaces, and it has NO effect on methods — see {@link Logged}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface SecureService {

    String value() default "secured";
}
