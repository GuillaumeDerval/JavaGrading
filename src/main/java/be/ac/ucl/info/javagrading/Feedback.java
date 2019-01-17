package be.ac.ucl.info.javagrading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Feedback {
    abstract class FeedbackClass<T> {
        public abstract String apply(T obj, TestStatus status);
    }
    Class<? extends FeedbackClass<?>> use();
}