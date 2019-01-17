package be.ac.ucl.info.javagrading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Grade {
    double value() default 1.0;
    long cputimeout() default -1L; //in ms
}