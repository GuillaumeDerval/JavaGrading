package com.github.guillaumederval.javagrading;

import com.github.guillaumederval.javagrading.utils.PrintPermission;
import org.junit.Test;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.security.cert.Certificate;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Functions common to GradingRunners.
 *
 * Does all the hard work about stdout/stderr and cpu timeouts.
 */
class GradingRunnerUtils {
    static Statement methodInvoker(FrameworkMethod method, Statement base) {
        return cpu(method, jail(method, base));
    }

    static Statement methodBlock(FrameworkMethod method, Statement base) {
        return base;
    }

    static Statement withPotentialTimeout(FrameworkMethod method, Object test, Statement next) {
        Test annoTest = method.getAnnotation(Test.class);
        Grade annoGrade = method.getAnnotation(Grade.class);
        GradeClass annoGradeClass = method.getDeclaringClass().getAnnotation(GradeClass.class);

        long timeout = 0;

        if(annoTest != null)
            timeout = annoTest.timeout();

        if(annoGrade != null && timeout == 0 && annoGrade.cpuTimeout() > 0)
            timeout = annoGrade.cpuTimeout() * 3;

        if(annoGradeClass != null && timeout == 0 && annoGradeClass.defaultCpuTimeout() > 0)
            timeout = annoGradeClass.defaultCpuTimeout() * 3;

        if (timeout <= 0) {
            return next;
        }
        return FailOnTimeout.builder()
                .withTimeout(timeout, TimeUnit.MILLISECONDS)
                .build(next);
    }

    /**
     * Add a test that verifies that a given test do not take too much cpu time
     */
    private static Statement cpu(final FrameworkMethod method, final Statement base) {
        Grade g = method.getAnnotation(Grade.class);
        GradeClass gc = method.getDeclaringClass().getAnnotation(GradeClass.class);

        long cpuTimeout = 0;
        if(g != null && g.cpuTimeout() > 0)
            cpuTimeout = g.cpuTimeout();
        if(gc != null && cpuTimeout == 0 && gc.defaultCpuTimeout() > 0)
            cpuTimeout = gc.defaultCpuTimeout();

        final long cpuTimeoutFinal = cpuTimeout;
        final boolean debug = g != null && g.debug();

        if(cpuTimeoutFinal > 0) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    ThreadMXBean thread = ManagementFactory.getThreadMXBean();
                    long start = thread.getCurrentThreadCpuTime();
                    base.evaluate();
                    long end = thread.getCurrentThreadCpuTime();
                    if(debug)
                        System.out.println("Function "+ method.toString()+ " took " + ((end-start)/1000000L) + "ms");
                    if(end-start > cpuTimeoutFinal*1000000L)
                        throw new TestTimedOutException(cpuTimeoutFinal, MILLISECONDS);
                }
            };
        }
        else
            return base;
    }

    private static Statement jail(FrameworkMethod method, final Statement base) {
        checkSecurity();

        final Grade g = method.getAnnotation(Grade.class);

        PermissionCollection coll = null;
        if(g != null) {
            try {
                coll = g.customPermissions().getConstructor().newInstance().get();
            }
            catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ignored) {
                //ignored
            }
        }

        if(coll == null)
            coll = new Permissions();
        if(g != null && g.debug())
            coll.add(PrintPermission.instance);

        ProtectionDomain pd = new ProtectionDomain(new CodeSource(null, (Certificate[]) null), coll);

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {

                Throwable ex = AccessController.doPrivileged(new PrivilegedExceptionAction<Throwable>() {
                    @Override
                    public Throwable run() throws Exception {
                        Throwable ex = null;
                        try {
                            base.evaluate();
                        } catch (Throwable throwable) {
                            ex = throwable;
                        }
                        return ex;
                    }
                }, new AccessControlContext(new ProtectionDomain[]{pd}));

                if(ex != null)
                    throw ex;
            }
        };
    }

    private static void checkSecurity() {
        if(!(System.getSecurityManager() instanceof TestSecurityManager)) {
            try {
                System.setSecurityManager(new TestSecurityManager());
            }
            catch (SecurityException e) {
                System.out.println("/!\\ WARNING: Cannot set a TestSecurityManager as the security manager. Tests may not be jailed properly.");
            }
        }
    }
}
