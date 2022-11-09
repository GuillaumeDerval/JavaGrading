package com.github.guillaumederval.javagrading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Grade {
    /**
     * Value for the test
     */
    double value() default 1.0;

    /**
     * CPU timeout in ms. Does not kill the submission, but measures it the time is ok afterwards.
     * Should be used with @Test(timeout=xxx). If timeout is not set on @Test, the default will be 3*cpuTimeout
     */
    long cpuTimeout() default 0L;

    /**
     * Output cputime info, allow printing on stdout/stderr
     */
    boolean debug() default false;

    /**
     * Expects a CustomGradingResult?
     * If false, CustomGradingResult will be considered as a standard error
     */
    boolean custom() default false;

}