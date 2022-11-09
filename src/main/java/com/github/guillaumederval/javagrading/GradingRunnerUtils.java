package com.github.guillaumederval.javagrading;

import org.junit.Test;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Functions common to GradingRunners.
 *
 * Does all the hard work about stdout/stderr and cpu timeouts.
 */
class GradingRunnerUtils {
    static Statement methodInvoker(FrameworkMethod method, Statement base) {
        return cpu(method, base);
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

}
