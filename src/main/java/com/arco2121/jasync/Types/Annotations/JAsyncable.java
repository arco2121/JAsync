package com.arco2121.jasync.Types.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declare a method as an Asycn function, similar to Async.async(() -> {}) or (() -> {}, delay)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JAsyncable {

    long delay() default 0;
}
