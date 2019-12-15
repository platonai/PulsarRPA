package org.h2.ext.pulsar.annotation;

import java.lang.annotation.*;

/**
 * This annotation is used to inject information into a class
 * field, bean property or method parameter.
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface H2Context {
}
