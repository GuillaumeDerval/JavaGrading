package be.ac.ucl.info.javagrading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface GradeClass {
    /**
     * The total value attributed to this test class.
     *
     * If -1, then each test will keep its original value.
     * Else, the total for all the tests in this class will be divided by the maximum value and
     * multiplied by totalValue.
     */
    double totalValue() default -1.0;

    /**
     * Default value for each test in the class.
     */
    double defaultValue() default 1.0;

    /**
     * If set to true, then all tests in the class must be ok to receive the grade
     */
    boolean allCorrect() default false;
}