package com.github.guillaumederval.javagrading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.PermissionCollection;

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

    /**
     * Overrides permissions. Not token into accound if PermissionCollectionFactory.get() returns null.
     *
     * The class should be instantiable without args.
     *
     * By default, tests have no particular permissions, i.e. they can't do anything fancy with the JVM.
     *
     * Note: if you allow modifyThreadGroup/modifyThread and setIO, you may break some components of JavaGrading,
     * namely the proctection against stdout/stderr usage and the cpu timeout management. Reflection is also a problem,
     * and other permissions may allow tests to jailbreak. Use with caution.
     */
    Class<? extends PermissionCollectionFactory> customPermissions() default NullPermissionCollectionFactory.class;

    interface PermissionCollectionFactory {
        PermissionCollection get();
    }

    class NullPermissionCollectionFactory implements PermissionCollectionFactory {
        @Override
        public PermissionCollection get() {
            return null;
        }
    }
}