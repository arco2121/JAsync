package com.arco2121.jasync.Types.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declare a method ad an Await function similar to Async.await(taskReturn) or (task, timeout)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JAwaitable {

    long timeout() default 0;
}
