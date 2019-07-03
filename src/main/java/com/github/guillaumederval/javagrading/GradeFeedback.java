package com.github.guillaumederval.javagrading;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Repeatable(GradeFeedbacks.class)
public @interface GradeFeedback {
    /**
     * Message. Use $trace to put the trace or $exception to put the exception.
     * $trace and $exception must be alone on their line, with possible whitespaces before them (that will be copied)
     */
    String message();

    /**
     * By default, show on failure and timeout.
     * If you put any of these to true, then it will only be displayed in this case.
     */
    boolean onFail() default false;
    boolean onTimeout() default false;
    boolean onSuccess() default false;
    boolean onIgnore() default false;
}